package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.modules.voucher.dto.SeckillOrderMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SeckillMonitorMetricsService {

    private static final int RECENT_FAILURE_LIMIT = 50;

    private final AtomicLong submitRequestCount = new AtomicLong();
    private final AtomicLong acceptedCount = new AtomicLong();
    private final AtomicLong luaRejectStock = new AtomicLong();
    private final AtomicLong luaRejectDuplicate = new AtomicLong();
    private final AtomicLong luaRejectNotStarted = new AtomicLong();
    private final AtomicLong luaRejectEnded = new AtomicLong();
    private final AtomicLong luaRejectUnknown = new AtomicLong();

    private final AtomicLong mqPublishSuccessCount = new AtomicLong();
    private final AtomicLong mqPublishFailCount = new AtomicLong();
    private final AtomicLong fallbackCount = new AtomicLong();

    private final AtomicLong processSuccessCount = new AtomicLong();
    private final AtomicLong processFailCount = new AtomicLong();
    private final AtomicLong retryCount = new AtomicLong();
    private final AtomicLong deadLetterCount = new AtomicLong();

    private final AtomicLong totalProcessLatencyMs = new AtomicLong();
    private final AtomicLong maxProcessLatencyMs = new AtomicLong();
    private final AtomicLong totalQueueDelayMs = new AtomicLong();
    private final AtomicLong maxQueueDelayMs = new AtomicLong();
    private final AtomicLong queueDelayCount = new AtomicLong();

    private final ConcurrentHashMap<Long, VoucherMetric> voucherMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failReasonCounter = new ConcurrentHashMap<>();
    private final Deque<Map<String, Object>> recentFailures = new ArrayDeque<>();

    public void recordSubmit(Long voucherId) {
        submitRequestCount.incrementAndGet();
        metric(voucherId).submit.incrementAndGet();
    }

    public void recordLuaReject(Long voucherId, Long luaCode) {
        if (luaCode == null) {
            luaRejectUnknown.incrementAndGet();
            metric(voucherId).rejectUnknown.incrementAndGet();
            return;
        }
        if (luaCode == 1L) {
            luaRejectStock.incrementAndGet();
            metric(voucherId).rejectStock.incrementAndGet();
            return;
        }
        if (luaCode == 2L) {
            luaRejectDuplicate.incrementAndGet();
            metric(voucherId).rejectDuplicate.incrementAndGet();
            return;
        }
        if (luaCode == 3L) {
            luaRejectNotStarted.incrementAndGet();
            metric(voucherId).rejectNotStarted.incrementAndGet();
            return;
        }
        if (luaCode == 4L) {
            luaRejectEnded.incrementAndGet();
            metric(voucherId).rejectEnded.incrementAndGet();
            return;
        }
        luaRejectUnknown.incrementAndGet();
        metric(voucherId).rejectUnknown.incrementAndGet();
    }

    public void recordAccepted(Long voucherId) {
        acceptedCount.incrementAndGet();
        metric(voucherId).accepted.incrementAndGet();
    }

    public void recordMqPublishSuccess(Long voucherId) {
        mqPublishSuccessCount.incrementAndGet();
        metric(voucherId).mqPublishSuccess.incrementAndGet();
    }

    public void recordMqPublishFail(Long voucherId, String reason) {
        mqPublishFailCount.incrementAndGet();
        metric(voucherId).mqPublishFail.incrementAndGet();
        addFailure(voucherId, null, normalizeReason(reason), "mq_publish");
    }

    public void recordFallback(Long voucherId) {
        fallbackCount.incrementAndGet();
        metric(voucherId).fallback.incrementAndGet();
    }

    public void recordProcessSuccess(SeckillOrderMessage message, long processLatencyMs, String source) {
        Long voucherId = message == null ? null : message.getVoucherId();
        processSuccessCount.incrementAndGet();
        metric(voucherId).processSuccess.incrementAndGet();
        addLatency(totalProcessLatencyMs, maxProcessLatencyMs, processLatencyMs);

        if (message != null) {
            long queueDelayMs = Math.max(0L, Instant.now().getEpochSecond() - message.getCreateEpochSecond()) * 1000L;
            totalQueueDelayMs.addAndGet(queueDelayMs);
            queueDelayCount.incrementAndGet();
            updateMax(maxQueueDelayMs, queueDelayMs);
        }

        if ("fallback".equals(source)) {
            metric(voucherId).fallbackProcessSuccess.incrementAndGet();
        } else {
            metric(voucherId).consumerProcessSuccess.incrementAndGet();
        }
    }

    public void recordProcessFail(SeckillOrderMessage message, String reason, String source) {
        Long voucherId = message == null ? null : message.getVoucherId();
        String orderNo = message == null ? null : message.getOrderNo();
        processFailCount.incrementAndGet();
        metric(voucherId).processFail.incrementAndGet();
        addFailure(voucherId, orderNo, normalizeReason(reason), source);
    }

    public void recordRetry(SeckillOrderMessage message) {
        Long voucherId = message == null ? null : message.getVoucherId();
        retryCount.incrementAndGet();
        metric(voucherId).retry.incrementAndGet();
    }

    public void recordDeadLetter(SeckillOrderMessage message) {
        Long voucherId = message == null ? null : message.getVoucherId();
        deadLetterCount.incrementAndGet();
        metric(voucherId).deadLetter.incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("submitRequestCount", submitRequestCount.get());
        data.put("acceptedCount", acceptedCount.get());
        data.put("luaRejectStock", luaRejectStock.get());
        data.put("luaRejectDuplicate", luaRejectDuplicate.get());
        data.put("luaRejectNotStarted", luaRejectNotStarted.get());
        data.put("luaRejectEnded", luaRejectEnded.get());
        data.put("luaRejectUnknown", luaRejectUnknown.get());
        data.put("mqPublishSuccessCount", mqPublishSuccessCount.get());
        data.put("mqPublishFailCount", mqPublishFailCount.get());
        data.put("fallbackCount", fallbackCount.get());
        data.put("processSuccessCount", processSuccessCount.get());
        data.put("processFailCount", processFailCount.get());
        data.put("retryCount", retryCount.get());
        data.put("deadLetterCount", deadLetterCount.get());
        data.put("processSuccessRate", percent(processSuccessCount.get(), processSuccessCount.get() + processFailCount.get()));
        data.put("acceptRate", percent(acceptedCount.get(), submitRequestCount.get()));
        data.put("avgProcessLatencyMs", avg(totalProcessLatencyMs.get(), processSuccessCount.get() + processFailCount.get()));
        data.put("maxProcessLatencyMs", maxProcessLatencyMs.get());
        data.put("avgQueueDelayMs", avg(totalQueueDelayMs.get(), queueDelayCount.get()));
        data.put("maxQueueDelayMs", maxQueueDelayMs.get());
        data.put("topFailReasons", topFailReasons());
        data.put("recentFailures", recentFailureList());
        data.put("voucherMetrics", perVoucherSnapshot());
        return data;
    }

    public void reset() {
        submitRequestCount.set(0);
        acceptedCount.set(0);
        luaRejectStock.set(0);
        luaRejectDuplicate.set(0);
        luaRejectNotStarted.set(0);
        luaRejectEnded.set(0);
        luaRejectUnknown.set(0);
        mqPublishSuccessCount.set(0);
        mqPublishFailCount.set(0);
        fallbackCount.set(0);
        processSuccessCount.set(0);
        processFailCount.set(0);
        retryCount.set(0);
        deadLetterCount.set(0);
        totalProcessLatencyMs.set(0);
        maxProcessLatencyMs.set(0);
        totalQueueDelayMs.set(0);
        maxQueueDelayMs.set(0);
        queueDelayCount.set(0);
        voucherMetrics.clear();
        failReasonCounter.clear();
        synchronized (recentFailures) {
            recentFailures.clear();
        }
    }

    private List<Map<String, Object>> topFailReasons() {
        List<Map<String, Object>> list = new ArrayList<>();
        failReasonCounter.forEach((k, v) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("reason", k);
            item.put("count", v.get());
            list.add(item);
        });
        list.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        if (list.size() > 6) {
            return list.subList(0, 6);
        }
        return list;
    }

    private List<Map<String, Object>> recentFailureList() {
        synchronized (recentFailures) {
            return new ArrayList<>(recentFailures);
        }
    }

    private List<Map<String, Object>> perVoucherSnapshot() {
        List<Map<String, Object>> list = new ArrayList<>();
        voucherMetrics.forEach((voucherId, metric) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("voucherId", voucherId);
            item.put("submit", metric.submit.get());
            item.put("accepted", metric.accepted.get());
            item.put("mqPublishSuccess", metric.mqPublishSuccess.get());
            item.put("mqPublishFail", metric.mqPublishFail.get());
            item.put("processSuccess", metric.processSuccess.get());
            item.put("processFail", metric.processFail.get());
            item.put("retry", metric.retry.get());
            item.put("deadLetter", metric.deadLetter.get());
            item.put("fallback", metric.fallback.get());
            list.add(item);
        });
        list.sort((a, b) -> Long.compare((Long) b.get("submit"), (Long) a.get("submit")));
        if (list.size() > 20) {
            return list.subList(0, 20);
        }
        return list;
    }

    private void addFailure(Long voucherId, String orderNo, String reason, String source) {
        failReasonCounter.computeIfAbsent(reason, k -> new AtomicLong()).incrementAndGet();
        Map<String, Object> item = new HashMap<>();
        item.put("time", Instant.now().toString());
        item.put("voucherId", voucherId);
        item.put("orderNo", orderNo);
        item.put("reason", reason);
        item.put("source", source);
        synchronized (recentFailures) {
            recentFailures.addFirst(item);
            while (recentFailures.size() > RECENT_FAILURE_LIMIT) {
                recentFailures.removeLast();
            }
        }
    }

    private VoucherMetric metric(Long voucherId) {
        return voucherMetrics.computeIfAbsent(voucherId == null ? -1L : voucherId, key -> new VoucherMetric());
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "未知错误";
        }
        String value = reason.trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private void addLatency(AtomicLong total, AtomicLong max, long latencyMs) {
        long safe = Math.max(0L, latencyMs);
        total.addAndGet(safe);
        updateMax(max, safe);
    }

    private void updateMax(AtomicLong max, long value) {
        long prev = max.get();
        while (value > prev && !max.compareAndSet(prev, value)) {
            prev = max.get();
        }
    }

    private double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    private double avg(long total, long count) {
        if (count <= 0) {
            return 0D;
        }
        return Math.round((total * 100.0 / count)) / 100.0;
    }

    private static class VoucherMetric {
        private final AtomicLong submit = new AtomicLong();
        private final AtomicLong accepted = new AtomicLong();
        private final AtomicLong rejectStock = new AtomicLong();
        private final AtomicLong rejectDuplicate = new AtomicLong();
        private final AtomicLong rejectNotStarted = new AtomicLong();
        private final AtomicLong rejectEnded = new AtomicLong();
        private final AtomicLong rejectUnknown = new AtomicLong();
        private final AtomicLong mqPublishSuccess = new AtomicLong();
        private final AtomicLong mqPublishFail = new AtomicLong();
        private final AtomicLong fallback = new AtomicLong();
        private final AtomicLong fallbackProcessSuccess = new AtomicLong();
        private final AtomicLong consumerProcessSuccess = new AtomicLong();
        private final AtomicLong processSuccess = new AtomicLong();
        private final AtomicLong processFail = new AtomicLong();
        private final AtomicLong retry = new AtomicLong();
        private final AtomicLong deadLetter = new AtomicLong();
    }
}
