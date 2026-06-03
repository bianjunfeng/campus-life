package com.campus.campus_life_backend.modules.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.campus.campus_life_backend.modules.search.dto.SearchSyncCandidateDTO;
import com.campus.campus_life_backend.modules.search.dto.SearchUserItemDTO;
import com.campus.campus_life_backend.modules.search.dto.SearchUserResultDTO;
import com.campus.campus_life_backend.modules.search.dto.UserSearchSourceDTO;
import com.campus.campus_life_backend.modules.search.es.UserSearchDocument;
import com.campus.campus_life_backend.modules.search.event.UserSearchEvent;
import com.campus.campus_life_backend.modules.search.repository.UserSearchRepository;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserSearchService {

    private static final Logger log = LoggerFactory.getLogger(UserSearchService.class);
    private static final int REBUILD_BATCH_SIZE = 200;

    private final UserMapper userMapper;
    private final ObjectProvider<UserSearchRepository> userSearchRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final boolean esEnabled;

    public UserSearchService(
            UserMapper userMapper,
            ObjectProvider<UserSearchRepository> userSearchRepositoryProvider,
            ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
            @Value("${search.es.enabled:false}") boolean esEnabled
    ) {
        this.userMapper = userMapper;
        this.userSearchRepositoryProvider = userSearchRepositoryProvider;
        this.elasticsearchOperationsProvider = elasticsearchOperationsProvider;
        this.esEnabled = esEnabled;
    }

    public void syncUserToEs(Long userId) {
        syncUserToEs(userId, resolveSyncVersion(userId));
    }

    public void syncUserToEs(Long userId, Long syncVersion) {
        if (userId == null || !esEnabled) {
            return;
        }
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        UserSearchRepository repository = userSearchRepositoryProvider.getIfAvailable();
        if (operations == null || repository == null) {
            log.warn("同步用户ES跳过，ES客户端或仓储不可用");
            return;
        }

        UserSearchSourceDTO source = userMapper.findSearchUserById(userId);
        if (source == null || source.getStatus() == null || source.getStatus() != 1) {
            deleteUserFromEs(userId, syncVersion);
            return;
        }

        UserSearchDocument existing = repository.findById(userId).orElse(null);
        if (existing != null && existing.getSyncVersion() != null && syncVersion != null
                && existing.getSyncVersion() > syncVersion) {
            log.info("忽略过期的用户索引同步事件 userId={}, currentVersion={}, eventVersion={}",
                    userId, existing.getSyncVersion(), syncVersion);
            return;
        }

        repository.save(buildDocument(source, syncVersion));
    }

    public void deleteUserFromEs(Long userId) {
        deleteUserFromEs(userId, resolveSyncVersion(userId));
    }

    public void deleteUserFromEs(Long userId, Long syncVersion) {
        if (userId == null || !esEnabled) {
            return;
        }
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        UserSearchRepository repository = userSearchRepositoryProvider.getIfAvailable();
        if (operations == null || repository == null) {
            log.warn("删除用户ES文档跳过，ES客户端或仓储不可用");
            return;
        }
        UserSearchDocument existing = repository.findById(userId).orElse(null);
        if (existing == null) {
            return;
        }
        if (existing.getSyncVersion() != null && syncVersion != null && existing.getSyncVersion() > syncVersion) {
            log.info("忽略过期的用户索引删除事件 userId={}, currentVersion={}, eventVersion={}",
                    userId, existing.getSyncVersion(), syncVersion);
            return;
        }
        repository.deleteById(userId);
    }

    public void applyEvent(UserSearchEvent event) {
        if (event == null || event.getUserId() == null) {
            return;
        }
        Long syncVersion = event.getSyncVersion() == null ? event.getTimestamp() : event.getSyncVersion();
        if ("DELETE".equalsIgnoreCase(event.getAction())) {
            deleteUserFromEs(event.getUserId(), syncVersion);
            return;
        }
        syncUserToEs(event.getUserId(), syncVersion);
    }

    public Long resolveSyncVersion(Long userId) {
        if (userId == null) {
            return System.currentTimeMillis();
        }
        User user = userMapper.findById(userId);
        if (user != null && user.getUpdateTime() != null) {
            return toEpochMillis(user.getUpdateTime());
        }
        return System.currentTimeMillis();
    }

    public Map<String, Object> rebuildUserIndex() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("esEnabled", esEnabled);
        result.put("indexAlias", getAliasName());
        if (!esEnabled) {
            result.put("status", "SKIPPED");
            result.put("reason", "search.es.enabled=false");
            result.put("reindexedCount", 0);
            return result;
        }

        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        UserSearchRepository repository = userSearchRepositoryProvider.getIfAvailable();
        result.put("esClientAvailable", operations != null);
        result.put("repositoryAvailable", repository != null);
        if (operations == null || repository == null) {
            result.put("status", "SKIPPED");
            result.put("reason", "Elasticsearch client unavailable");
            result.put("reindexedCount", 0);
            return result;
        }

        try {
            String aliasName = getAliasName();
            String newIndex = aliasName + "_v" + System.currentTimeMillis();
            IndexOperations aliasOps = operations.indexOps(UserSearchDocument.class);
            IndexOperations targetOps = operations.indexOps(IndexCoordinates.of(newIndex));
            if (targetOps.exists()) {
                targetOps.delete();
            }
            targetOps.create(aliasOps.createSettings(UserSearchDocument.class), aliasOps.createMapping(UserSearchDocument.class));

            int offset = 0;
            int reindexedCount = 0;
            while (true) {
                List<Long> userIds = userMapper.findIndexableUserIds(offset, REBUILD_BATCH_SIZE);
                if (userIds == null || userIds.isEmpty()) {
                    break;
                }
                for (Long userId : userIds) {
                    UserSearchSourceDTO source = userMapper.findSearchUserById(userId);
                    if (source == null || source.getStatus() == null || source.getStatus() != 1) {
                        continue;
                    }
                    operations.save(buildDocument(source, resolveSyncVersion(userId)), IndexCoordinates.of(newIndex));
                    reindexedCount++;
                }
                offset += userIds.size();
            }

            switchAlias(aliasName, newIndex, aliasOps, operations);
            targetOps.refresh();
            result.put("status", "SUCCESS");
            result.put("reindexedCount", reindexedCount);
            result.put("batchSize", REBUILD_BATCH_SIZE);
            result.put("activeIndex", newIndex);
            return result;
        } catch (Exception e) {
            log.error("重建用户搜索索引失败", e);
            result.put("status", "FAILED");
            result.put("reason", e.getMessage());
            return result;
        }
    }

    public int compensateUpdatedUsers(LocalDateTime since, int batchSize) {
        if (!esEnabled || since == null || batchSize <= 0) {
            return 0;
        }
        int total = 0;
        int offset = 0;
        while (true) {
            List<SearchSyncCandidateDTO> candidates = userMapper.findUpdatedUserCandidates(since, offset, batchSize);
            if (candidates == null || candidates.isEmpty()) {
                break;
            }
            for (SearchSyncCandidateDTO candidate : candidates) {
                Long syncVersion = candidate.getUpdateTime() == null ? System.currentTimeMillis() : toEpochMillis(candidate.getUpdateTime());
                syncUserToEs(candidate.getId(), syncVersion);
                total++;
            }
            if (candidates.size() < batchSize) {
                break;
            }
            offset += candidates.size();
        }
        return total;
    }

    public SearchUserResultDTO searchUsers(String keyword, int page, int size) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        if (safeKeyword.isEmpty()) {
            return emptyResult(safePage, safeSize);
        }
        if (!esEnabled) {
            return safeMysqlFallback(safeKeyword, safePage, safeSize);
        }
        try {
            return searchWithEs(safeKeyword, safePage, safeSize);
        } catch (Exception e) {
            log.warn("ES用户搜索失败，降级MySQL keyword={}", safeKeyword, e);
            return safeMysqlFallback(safeKeyword, safePage, safeSize);
        }
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "user-search");
        result.put("fallback", "mysql+empty");
        result.put("esEnabled", esEnabled);
        result.put("indexAlias", getAliasName());

        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        UserSearchRepository repository = userSearchRepositoryProvider.getIfAvailable();
        boolean clientAvailable = operations != null;
        boolean repositoryAvailable = repository != null;
        boolean indexExists = false;
        long documentCount = 0L;
        String esStatus = "DISABLED";
        String searchMode = "mysql";

        if (esEnabled) {
            if (clientAvailable && repositoryAvailable) {
                try {
                    IndexOperations indexOperations = operations.indexOps(UserSearchDocument.class);
                    indexExists = indexOperations.exists();
                    if (indexExists) {
                        documentCount = repository.count();
                    }
                    esStatus = indexExists ? "UP" : "DEGRADED";
                    searchMode = indexExists ? "elasticsearch" : "mysql";
                } catch (Exception e) {
                    log.warn("检查用户搜索ES健康状态失败", e);
                    esStatus = "DEGRADED";
                }
            } else {
                esStatus = "DEGRADED";
            }
        }

        result.put("esStatus", esStatus);
        result.put("esClientAvailable", clientAvailable);
        result.put("repositoryAvailable", repositoryAvailable);
        result.put("indexExists", indexExists);
        result.put("documentCount", documentCount);
        result.put("searchMode", searchMode);
        return result;
    }

    private SearchUserResultDTO safeMysqlFallback(String keyword, int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<SearchUserItemDTO> list = userMapper.searchUsers(keyword, offset, size);
            long total = userMapper.countSearchUsers(keyword);
            SearchUserResultDTO result = new SearchUserResultDTO();
            result.setList(list);
            result.setTotal(total);
            result.setPage(page);
            result.setSize(size);
            return result;
        } catch (Exception e) {
            log.error("用户搜索失败，返回空结果 keyword={}", keyword, e);
            return emptyResult(page, size);
        }
    }

    private SearchUserResultDTO searchWithEs(String keyword, int page, int size) {
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        if (operations == null) {
            throw new IllegalStateException("ElasticsearchOperations 不可用");
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("status").value(1)))
                        .should(s -> s.match(m -> m.field("username").query(keyword).boost(5.0f)))
                        .should(s -> s.match(m -> m.field("bio").query(keyword).boost(2.0f)))
                        .should(s -> s.match(m -> m.field("phone").query(keyword).boost(1.2f)))
                        .minimumShouldMatch("1")
                ))
                .withHighlightQuery(buildHighlightQuery())
                .withTrackScores(true)
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("followerCount").order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("postCount").order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<UserSearchDocument> hits = operations.search(query, UserSearchDocument.class);
        List<SearchUserItemDTO> list = new ArrayList<>();
        for (SearchHit<UserSearchDocument> hit : hits) {
            UserSearchDocument doc = hit.getContent();
            if (doc.getStatus() == null || doc.getStatus() != 1) {
                continue;
            }
            SearchUserItemDTO item = new SearchUserItemDTO();
            item.setId(doc.getId());
            item.setUsername(firstHighlight(hit, "username", escapeHtml(doc.getUsername())));
            item.setAvatarUrl(doc.getAvatarUrl());
            item.setBio(firstHighlight(hit, "bio", escapeHtml(doc.getBio())));
            item.setRole(doc.getRole());
            item.setPostCount(doc.getPostCount());
            item.setFollowerCount(doc.getFollowerCount());
            item.setCreateTime(doc.getCreateTime());
            list.add(item);
        }

        SearchUserResultDTO result = new SearchUserResultDTO();
        result.setList(list);
        result.setTotal(hits.getTotalHits());
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    private HighlightQuery buildHighlightQuery() {
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("<em class='search-hit'>")
                .withPostTags("</em>")
                .withRequireFieldMatch(false)
                .withFragmentSize(80)
                .withNoMatchSize(80)
                .build();
        Highlight highlight = new Highlight(parameters, List.of(
                new HighlightField("username"),
                new HighlightField("bio")
        ));
        return new HighlightQuery(highlight, UserSearchDocument.class);
    }

    private UserSearchDocument buildDocument(UserSearchSourceDTO source, Long syncVersion) {
        UserSearchDocument doc = new UserSearchDocument();
        doc.setId(source.getId());
        doc.setUsername(source.getUsername());
        doc.setAvatarUrl(source.getAvatarUrl());
        doc.setBio(source.getBio());
        doc.setPhone(source.getPhone());
        doc.setRole(source.getRole());
        doc.setStatus(source.getStatus());
        doc.setPostCount(source.getPostCount());
        doc.setFollowerCount(source.getFollowerCount());
        doc.setCreateTime(source.getCreateTime());
        doc.setUpdateTime(source.getUpdateTime());
        doc.setSyncVersion(syncVersion == null ? System.currentTimeMillis() : syncVersion);
        return doc;
    }

    private String getAliasName() {
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        if (operations == null) {
            return "forum_user";
        }
        IndexCoordinates indexCoordinates = operations.getIndexCoordinatesFor(UserSearchDocument.class);
        return indexCoordinates == null ? "forum_user" : indexCoordinates.getIndexName();
    }

    private void switchAlias(String aliasName, String newIndex, IndexOperations aliasOps, ElasticsearchOperations operations) {
        Map<String, Set<org.springframework.data.elasticsearch.core.index.AliasData>> aliases = safeGetAliases(aliasOps, aliasName);
        org.springframework.data.elasticsearch.core.index.AliasActions actions =
                new org.springframework.data.elasticsearch.core.index.AliasActions(
                        new org.springframework.data.elasticsearch.core.index.AliasAction.Add(
                                org.springframework.data.elasticsearch.core.index.AliasActionParameters.builder()
                                        .withIndices(newIndex)
                                        .withAliases(aliasName)
                                        .withIsWriteIndex(true)
                                        .build()
                        )
                );

        List<String> oldIndexes = new ArrayList<>();
        if (aliases == null || aliases.isEmpty()) {
            IndexOperations directOps = operations.indexOps(IndexCoordinates.of(aliasName));
            if (directOps.exists()) {
                actions.add(new org.springframework.data.elasticsearch.core.index.AliasAction.RemoveIndex(
                        org.springframework.data.elasticsearch.core.index.AliasActionParameters.builder()
                                .withIndices(aliasName)
                                .build()
                ));
                oldIndexes.add(aliasName);
            }
        } else {
            for (String oldIndex : aliases.keySet()) {
                if (!newIndex.equals(oldIndex)) {
                    oldIndexes.add(oldIndex);
                    actions.add(new org.springframework.data.elasticsearch.core.index.AliasAction.Remove(
                            org.springframework.data.elasticsearch.core.index.AliasActionParameters.builder()
                                    .withIndices(oldIndex)
                                    .withAliases(aliasName)
                                    .build()
                    ));
                }
            }
        }

        aliasOps.alias(actions);
        for (String oldIndex : oldIndexes) {
            if (!aliasName.equals(oldIndex)) {
                operations.indexOps(IndexCoordinates.of(oldIndex)).delete();
            }
        }
    }

    private Map<String, Set<org.springframework.data.elasticsearch.core.index.AliasData>> safeGetAliases(
            IndexOperations aliasOps,
            String aliasName
    ) {
        try {
            return aliasOps.getAliases(aliasName);
        } catch (ResourceNotFoundException ex) {
            return Collections.emptyMap();
        }
    }

    private long toEpochMillis(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String firstHighlight(SearchHit<UserSearchDocument> hit, String field, String fallback) {
        List<String> highlighted = hit.getHighlightField(field);
        if (highlighted == null || highlighted.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return String.join("", highlighted);
    }

    private String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private SearchUserResultDTO emptyResult(int page, int size) {
        SearchUserResultDTO result = new SearchUserResultDTO();
        result.setList(Collections.emptyList());
        result.setTotal(0);
        result.setPage(page);
        result.setSize(size);
        return result;
    }
}
