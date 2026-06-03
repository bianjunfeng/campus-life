package com.campus.campus_life_backend.modules.search.service;

import com.campus.campus_life_backend.common.event.mapper.SearchSyncCheckpointMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SearchIndexCompensationTask {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexCompensationTask.class);

    private final PostSearchService postSearchService;
    private final UserSearchService userSearchService;
    private final SearchSyncCheckpointMapper checkpointMapper;
    private final boolean enabled;
    private final int batchSize;
    private final long overlapSeconds;
    private final long initialLookbackSeconds;
    private final String checkpointName;
    private final AtomicLong lastRunStartedAt = new AtomicLong();
    private final AtomicLong lastRunFinishedAt = new AtomicLong();
    private final AtomicLong lastCheckpointMillis;
    private final AtomicLong lastPostSyncedCount = new AtomicLong();
    private final AtomicLong lastUserSyncedCount = new AtomicLong();
    private final AtomicBoolean checkpointLoaded = new AtomicBoolean(false);
    private volatile String lastError;

    public SearchIndexCompensationTask(
            PostSearchService postSearchService,
            UserSearchService userSearchService,
            ObjectProvider<SearchSyncCheckpointMapper> checkpointMapperProvider,
            @Value("${search.compensation.enabled:true}") boolean enabled,
            @Value("${search.compensation.batch-size:200}") int batchSize,
            @Value("${search.compensation.overlap-seconds:60}") long overlapSeconds,
            @Value("${search.compensation.initial-lookback-seconds:86400}") long initialLookbackSeconds,
            @Value("${search.compensation.checkpoint-name:search-index-compensation}") String checkpointName
    ) {
        this.postSearchService = postSearchService;
        this.userSearchService = userSearchService;
        this.checkpointMapper = checkpointMapperProvider == null ? null : checkpointMapperProvider.getIfAvailable();
        this.enabled = enabled;
        this.batchSize = Math.max(batchSize, 1);
        this.overlapSeconds = Math.max(overlapSeconds, 0);
        this.initialLookbackSeconds = Math.max(initialLookbackSeconds, 60);
        this.checkpointName = checkpointName == null || checkpointName.isBlank()
                ? "search-index-compensation"
                : checkpointName;
        this.lastCheckpointMillis = new AtomicLong(System.currentTimeMillis() - this.initialLookbackSeconds * 1000);
    }

    @Scheduled(
            initialDelayString = "${search.compensation.initial-delay-ms:15000}",
            fixedDelayString = "${search.compensation.fixed-delay-ms:300000}"
    )
    public void compensate() {
        if (!enabled) {
            return;
        }
        loadCheckpointIfNecessary();
        long startedAt = System.currentTimeMillis();
        lastRunStartedAt.set(startedAt);
        long sinceMillis = Math.max(0L, lastCheckpointMillis.get() - overlapSeconds * 1000);
        LocalDateTime since = LocalDateTime.ofInstant(Instant.ofEpochMilli(sinceMillis), ZoneId.systemDefault());
        try {
            int postCount = postSearchService.compensateUpdatedPosts(since, batchSize);
            int userCount = userSearchService.compensateUpdatedUsers(since, batchSize);
            lastPostSyncedCount.set(postCount);
            lastUserSyncedCount.set(userCount);
            lastCheckpointMillis.set(startedAt);
            persistCheckpoint(startedAt);
            lastError = null;
            log.info("搜索索引补偿完成 since={}, postCount={}, userCount={}", since, postCount, userCount);
        } catch (Exception e) {
            lastError = e.getMessage();
            markCheckpointFailure(e);
            log.error("搜索索引补偿失败 since={}", since, e);
        } finally {
            lastRunFinishedAt.set(System.currentTimeMillis());
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("batchSize", batchSize);
        result.put("overlapSeconds", overlapSeconds);
        result.put("initialLookbackSeconds", initialLookbackSeconds);
        result.put("checkpointName", checkpointName);
        result.put("checkpointLoaded", checkpointLoaded.get());
        result.put("lastCheckpointMillis", lastCheckpointMillis.get());
        result.put("lastRunStartedAt", lastRunStartedAt.get());
        result.put("lastRunFinishedAt", lastRunFinishedAt.get());
        result.put("lastPostSyncedCount", lastPostSyncedCount.get());
        result.put("lastUserSyncedCount", lastUserSyncedCount.get());
        result.put("lastError", lastError);
        return result;
    }

    private void loadCheckpointIfNecessary() {
        if (!checkpointLoaded.compareAndSet(false, true) || checkpointMapper == null) {
            return;
        }
        try {
            Long saved = checkpointMapper.findCheckpointMillis(checkpointName);
            if (saved != null && saved > 0) {
                lastCheckpointMillis.set(saved);
            }
        } catch (Exception e) {
            checkpointLoaded.set(false);
            log.warn("读取搜索补偿 checkpoint 失败，继续使用内存 checkpoint name={}", checkpointName, e);
        }
    }

    private void persistCheckpoint(long checkpointMillis) {
        if (checkpointMapper == null) {
            return;
        }
        try {
            checkpointMapper.upsertSuccess(checkpointName, checkpointMillis);
        } catch (Exception e) {
            log.warn("保存搜索补偿 checkpoint 失败 name={}", checkpointName, e);
        }
    }

    private void markCheckpointFailure(Exception error) {
        if (checkpointMapper == null) {
            return;
        }
        try {
            String message = error == null || error.getMessage() == null ? "" : error.getMessage();
            checkpointMapper.markFailure(checkpointName, message.length() > 1000 ? message.substring(0, 1000) : message);
        } catch (Exception ignored) {
        }
    }
}
