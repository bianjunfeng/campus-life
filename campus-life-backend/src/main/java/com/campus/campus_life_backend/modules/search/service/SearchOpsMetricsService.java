package com.campus.campus_life_backend.modules.search.service;

import com.campus.campus_life_backend.common.event.EventFailureRecord;
import com.campus.campus_life_backend.common.event.mapper.EventFailureRecordMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.LongAdder;

@Service
public class SearchOpsMetricsService {

    public static final String CHANNEL_POST_SEARCH = "post-search";
    public static final String CHANNEL_USER_SEARCH = "user-search";
    public static final String CHANNEL_NOTIFICATION = "notification";
    public static final String CHANNEL_VOUCHER_ORDER = "voucher-order";

    private final RedisTemplate<String, String> redisTemplate;
    private final EventFailureRecordMapper eventFailureRecordMapper;
    private final Duration retention;
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> localFailures = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<String>> localIndexes = new ConcurrentHashMap<>();

    public SearchOpsMetricsService(
            ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider,
            ObjectProvider<EventFailureRecordMapper> eventFailureRecordMapperProvider,
            @Value("${search.ops.failure-retention-hours:168}") long retentionHours
    ) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.eventFailureRecordMapper = eventFailureRecordMapperProvider.getIfAvailable();
        this.retention = Duration.ofHours(Math.max(retentionHours, 1));
    }

    public void recordProducerSuccess(String channel) {
        increment(channel + ".producer.success");
    }

    public void recordProducerFailure(String channel) {
        increment(channel + ".producer.failure");
    }

    public void recordConsumerSuccess(String channel) {
        increment(channel + ".consumer.success");
    }

    public void recordConsumerFailure(String channel) {
        increment(channel + ".consumer.failure");
    }

    public void recordDeadLetter(String channel, String originalTopic, String dltTopic, String key, String payload, Throwable error) {
        increment(channel + ".consumer.dlt");
        String recordId = UUID.randomUUID().toString();
        long failedAt = System.currentTimeMillis();
        Map<String, String> record = new LinkedHashMap<>();
        record.put("id", recordId);
        record.put("channel", channel);
        record.put("originalTopic", originalTopic == null ? "" : originalTopic);
        record.put("dltTopic", dltTopic == null ? "" : dltTopic);
        record.put("messageKey", key == null ? "" : key);
        record.put("payload", payload == null ? "" : payload);
        record.put("error", error == null ? "" : error.getMessage());
        record.put("failedAt", String.valueOf(failedAt));
        record.put("replayCount", "0");
        storeFailure(recordId, channel, failedAt, record);
    }

    public Map<String, Object> snapshot() {
        Map<String, Long> metrics = new LinkedHashMap<>();
        counters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> metrics.put(entry.getKey(), entry.getValue().sum()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metrics", metrics);
        result.put("retentionHours", retention.toHours());
        result.put("failureCounts", safeFailureCounts());
        return result;
    }

    public Map<String, Object> listFailures(String channel, Long startMillis, Long endMillis, int limit) {
        long start = startMillis == null ? 0L : startMillis;
        long end = endMillis == null ? Double.valueOf(Double.MAX_VALUE).longValue() : endMillis;
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<Map<String, Object>> dbList = readFailuresFromDatabase(channel, start, end, safeLimit);
        if (!dbList.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("channel", channel == null || channel.isBlank() ? "all" : channel);
            result.put("list", dbList);
            result.put("total", dbList.size());
            return result;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (String currentChannel : resolveChannels(channel)) {
            list.addAll(readFailures(currentChannel, start, end, safeLimit));
        }
        list.sort(Comparator.comparingLong((Map<String, Object> item) -> {
            Object failedAt = item.containsKey("failedAt") ? item.get("failedAt") : 0L;
            return Long.parseLong(String.valueOf(failedAt));
        }).reversed());
        if (list.size() > safeLimit) {
            list = new ArrayList<>(list.subList(0, safeLimit));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", channel == null || channel.isBlank() ? "all" : channel);
        result.put("list", list);
        result.put("total", list.size());
        return result;
    }

    public Map<String, String> getFailureRecord(String recordId) {
        if (recordId == null || recordId.isBlank()) {
            return null;
        }
        if (eventFailureRecordMapper != null) {
            try {
                EventFailureRecord dbRecord = eventFailureRecordMapper.findById(recordId);
                if (dbRecord != null) {
                    return toMap(dbRecord);
                }
            } catch (Exception ignored) {
            }
        }
        if (redisTemplate != null) {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(recordKey(recordId));
            if (entries != null && !entries.isEmpty()) {
                Map<String, String> result = new LinkedHashMap<>();
                entries.forEach((key, value) -> result.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
                return result;
            }
        }
        Map<String, String> local = localFailures.get(recordId);
        return local == null ? null : new LinkedHashMap<>(local);
    }

    public void markReplayResult(String recordId, boolean success, String message) {
        Map<String, String> updates = new HashMap<>();
        Map<String, String> record = getFailureRecord(recordId);
        int replayCount = parseInt(record == null ? null : record.get("replayCount")) + 1;
        updates.put("replayCount", String.valueOf(replayCount));
        updates.put("lastReplayStatus", success ? "SUCCESS" : "FAILED");
        updates.put("lastReplayAt", String.valueOf(System.currentTimeMillis()));
        updates.put("lastReplayMessage", message == null ? "" : message);
        if (eventFailureRecordMapper != null) {
            try {
                eventFailureRecordMapper.markReplayResult(recordId, success, message == null ? "" : message);
            } catch (Exception ignored) {
            }
        }
        if (redisTemplate != null) {
            redisTemplate.opsForHash().putAll(recordKey(recordId), updates);
            redisTemplate.expire(recordKey(recordId), retention);
        }
        Map<String, String> local = localFailures.get(recordId);
        if (local != null) {
            local.putAll(updates);
        }
    }

    public void markIgnored(String recordId, String message) {
        Map<String, String> updates = new HashMap<>();
        updates.put("lastReplayStatus", "IGNORED");
        updates.put("lastReplayAt", String.valueOf(System.currentTimeMillis()));
        updates.put("lastReplayMessage", message == null ? "" : message);
        updates.put("status", "IGNORED");
        if (eventFailureRecordMapper != null) {
            try {
                eventFailureRecordMapper.markIgnored(recordId, message == null ? "" : message);
            } catch (Exception ignored) {
            }
        }
        if (redisTemplate != null) {
            redisTemplate.opsForHash().putAll(recordKey(recordId), updates);
            redisTemplate.expire(recordKey(recordId), retention);
        }
        Map<String, String> local = localFailures.get(recordId);
        if (local != null) {
            local.putAll(updates);
        }
    }

    private void increment(String key) {
        counters.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    }

    private void storeFailure(String recordId, String channel, long failedAt, Map<String, String> record) {
        storeFailureInDatabase(recordId, channel, failedAt, record);
        if (redisTemplate != null) {
            redisTemplate.opsForHash().putAll(recordKey(recordId), record);
            redisTemplate.expire(recordKey(recordId), retention);
            redisTemplate.opsForZSet().add(indexKey(channel), recordId, failedAt);
            redisTemplate.expire(indexKey(channel), retention);
        }
        localFailures.put(recordId, new LinkedHashMap<>(record));
        localIndexes.computeIfAbsent(channel, ignored -> new ConcurrentLinkedDeque<>()).addFirst(recordId);
        pruneLocal(channel);
    }

    private void pruneLocal(String channel) {
        ConcurrentLinkedDeque<String> deque = localIndexes.get(channel);
        if (deque == null) {
            return;
        }
        while (deque.size() > 500) {
            String removed = deque.pollLast();
            if (removed != null) {
                localFailures.remove(removed);
            }
        }
    }

    private List<Map<String, Object>> readFailures(String channel, long start, long end, int limit) {
        if (redisTemplate != null) {
            Set<String> ids = redisTemplate.opsForZSet().reverseRangeByScore(indexKey(channel), start, end, 0, limit);
            if (ids != null && !ids.isEmpty()) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (String id : ids) {
                    Map<String, String> record = getFailureRecord(id);
                    if (record != null) {
                        list.add(new LinkedHashMap<>(record));
                    }
                }
                return list;
            }
        }

        ConcurrentLinkedDeque<String> deque = localIndexes.get(channel);
        if (deque == null || deque.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (String id : deque) {
            Map<String, String> record = localFailures.get(id);
            if (record == null) {
                continue;
            }
            long failedAt = parseLong(record.get("failedAt"));
            if (failedAt < start || failedAt > end) {
                continue;
            }
            list.add(new LinkedHashMap<>(record));
            if (list.size() >= limit) {
                break;
            }
        }
        return list;
    }

    private List<String> resolveChannels(String channel) {
        if (channel == null || channel.isBlank()) {
            return List.of(CHANNEL_POST_SEARCH, CHANNEL_USER_SEARCH, CHANNEL_NOTIFICATION, CHANNEL_VOUCHER_ORDER);
        }
        return List.of(channel);
    }

    private void storeFailureInDatabase(String recordId, String channel, long failedAt, Map<String, String> record) {
        if (eventFailureRecordMapper == null) {
            return;
        }
        try {
            EventFailureRecord dbRecord = new EventFailureRecord();
            dbRecord.setId(recordId);
            dbRecord.setChannel(channel);
            dbRecord.setOriginalTopic(record.get("originalTopic"));
            dbRecord.setDltTopic(record.get("dltTopic"));
            dbRecord.setMessageKey(record.get("messageKey"));
            dbRecord.setPayload(record.get("payload"));
            dbRecord.setError(record.get("error"));
            dbRecord.setFailedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(failedAt), ZoneId.systemDefault()));
            eventFailureRecordMapper.insert(dbRecord);
        } catch (Exception ignored) {
        }
    }

    private List<Map<String, Object>> readFailuresFromDatabase(String channel, long start, long end, int limit) {
        if (eventFailureRecordMapper == null) {
            return Collections.emptyList();
        }
        try {
            LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault());
            long boundedEnd = end == Double.valueOf(Double.MAX_VALUE).longValue() ? System.currentTimeMillis() : end;
            LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(boundedEnd), ZoneId.systemDefault());
            List<EventFailureRecord> records = eventFailureRecordMapper.list(
                    channel == null || channel.isBlank() ? null : channel,
                    startTime,
                    endTime,
                    limit
            );
            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> list = new ArrayList<>();
            for (EventFailureRecord record : records) {
                list.add(new LinkedHashMap<>(toMap(record)));
            }
            return list;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> safeFailureCounts() {
        if (eventFailureRecordMapper == null) {
            return Collections.emptyList();
        }
        try {
            return eventFailureRecordMapper.countByChannelAndStatus();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private Map<String, String> toMap(EventFailureRecord record) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", record.getId());
        result.put("channel", record.getChannel());
        result.put("originalTopic", record.getOriginalTopic() == null ? "" : record.getOriginalTopic());
        result.put("dltTopic", record.getDltTopic() == null ? "" : record.getDltTopic());
        result.put("messageKey", record.getMessageKey() == null ? "" : record.getMessageKey());
        result.put("payload", record.getPayload() == null ? "" : record.getPayload());
        result.put("error", record.getError() == null ? "" : record.getError());
        result.put("status", record.getStatus() == null ? "" : record.getStatus());
        result.put("failedAt", record.getFailedAt() == null
                ? "0"
                : String.valueOf(record.getFailedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        result.put("replayCount", record.getReplayCount() == null ? "0" : String.valueOf(record.getReplayCount()));
        result.put("lastReplayStatus", record.getLastReplayStatus() == null ? "" : record.getLastReplayStatus());
        result.put("lastReplayAt", record.getLastReplayAt() == null
                ? ""
                : String.valueOf(record.getLastReplayAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        result.put("lastReplayMessage", record.getLastReplayMessage() == null ? "" : record.getLastReplayMessage());
        return result;
    }

    private String recordKey(String recordId) {
        return "search:ops:failure:record:" + recordId;
    }

    private String indexKey(String channel) {
        return "search:ops:failure:index:" + channel;
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
