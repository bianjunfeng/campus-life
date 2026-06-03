package com.campus.campus_life_backend.modules.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.campus.campus_life_backend.modules.forum.dto.PostDTO;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.search.dto.SearchPostItemDTO;
import com.campus.campus_life_backend.modules.search.dto.SearchPostResultDTO;
import com.campus.campus_life_backend.modules.search.dto.SearchSyncCandidateDTO;
import com.campus.campus_life_backend.modules.search.es.PostSearchDocument;
import com.campus.campus_life_backend.modules.search.event.PostSearchEvent;
import com.campus.campus_life_backend.modules.search.repository.PostSearchRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostSearchService {

    private static final Logger log = LoggerFactory.getLogger(PostSearchService.class);
    private static final int REBUILD_BATCH_SIZE = 200;

    private final PostMapper postMapper;
    private final ObjectProvider<PostSearchRepository> postSearchRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final boolean esEnabled;

    public PostSearchService(
            PostMapper postMapper,
            ObjectProvider<PostSearchRepository> postSearchRepositoryProvider,
            ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
            @Value("${search.es.enabled:false}") boolean esEnabled
    ) {
        this.postMapper = postMapper;
        this.postSearchRepositoryProvider = postSearchRepositoryProvider;
        this.elasticsearchOperationsProvider = elasticsearchOperationsProvider;
        this.esEnabled = esEnabled;
    }

    public void syncPostToEs(Long postId) {
        syncPostToEs(postId, resolveSyncVersion(postId));
    }

    public void syncPostToEs(Long postId, Long syncVersion) {
        if (postId == null || !esEnabled) {
            return;
        }
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        PostSearchRepository repository = postSearchRepositoryProvider.getIfAvailable();
        if (operations == null || repository == null) {
            log.warn("同步ES跳过，ES客户端或仓储不可用");
            return;
        }

        Post rawPost = postMapper.findById(postId);
        if (rawPost == null || rawPost.getStatus() == null || rawPost.getStatus() != 0) {
            deletePostFromEs(postId, syncVersion);
            return;
        }

        PostDTO dto = postMapper.findByIdWithUser(postId);
        if (dto == null || dto.getStatus() == null || dto.getStatus() != 0) {
            deletePostFromEs(postId, syncVersion);
            return;
        }

        PostSearchDocument existing = repository.findById(postId).orElse(null);
        if (existing != null && existing.getSyncVersion() != null && syncVersion != null
                && existing.getSyncVersion() > syncVersion) {
            log.info("忽略过期的帖子索引同步事件 postId={}, currentVersion={}, eventVersion={}",
                    postId, existing.getSyncVersion(), syncVersion);
            return;
        }

        repository.save(buildDocument(dto, syncVersion));
    }

    public void deletePostFromEs(Long postId) {
        deletePostFromEs(postId, resolveSyncVersion(postId));
    }

    public void deletePostFromEs(Long postId, Long syncVersion) {
        if (postId == null || !esEnabled) {
            return;
        }
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        PostSearchRepository repository = postSearchRepositoryProvider.getIfAvailable();
        if (operations == null || repository == null) {
            log.warn("删除ES文档跳过，ES客户端或仓储不可用");
            return;
        }
        PostSearchDocument existing = repository.findById(postId).orElse(null);
        if (existing == null) {
            return;
        }
        if (existing.getSyncVersion() != null && syncVersion != null && existing.getSyncVersion() > syncVersion) {
            log.info("忽略过期的帖子索引删除事件 postId={}, currentVersion={}, eventVersion={}",
                    postId, existing.getSyncVersion(), syncVersion);
            return;
        }
        repository.deleteById(postId);
    }

    public void applyEvent(PostSearchEvent event) {
        if (event == null || event.getPostId() == null) {
            return;
        }
        Long syncVersion = event.getSyncVersion() == null ? event.getTimestamp() : event.getSyncVersion();
        if ("DELETE".equalsIgnoreCase(event.getAction())) {
            deletePostFromEs(event.getPostId(), syncVersion);
            return;
        }
        syncPostToEs(event.getPostId(), syncVersion);
    }

    public Long resolveSyncVersion(Long postId) {
        if (postId == null) {
            return System.currentTimeMillis();
        }
        Post post = postMapper.findById(postId);
        if (post != null && post.getUpdateTime() != null) {
            return toEpochMillis(post.getUpdateTime());
        }
        return System.currentTimeMillis();
    }

    public Map<String, Object> rebuildPostIndex() {
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
        PostSearchRepository repository = postSearchRepositoryProvider.getIfAvailable();
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
            IndexOperations aliasOps = operations.indexOps(PostSearchDocument.class);
            IndexOperations targetOps = operations.indexOps(IndexCoordinates.of(newIndex));
            if (targetOps.exists()) {
                targetOps.delete();
            }
            targetOps.create(aliasOps.createSettings(PostSearchDocument.class), aliasOps.createMapping(PostSearchDocument.class));

            int offset = 0;
            int reindexedCount = 0;
            while (true) {
                List<Long> postIds = postMapper.findIndexablePostIds(offset, REBUILD_BATCH_SIZE);
                if (postIds == null || postIds.isEmpty()) {
                    break;
                }
                for (Long postId : postIds) {
                    PostDTO dto = postMapper.findByIdWithUser(postId);
                    if (dto == null || dto.getStatus() == null || dto.getStatus() != 0) {
                        continue;
                    }
                    operations.save(buildDocument(dto, resolveSyncVersion(postId)), IndexCoordinates.of(newIndex));
                    reindexedCount++;
                }
                offset += postIds.size();
            }

            switchAlias(aliasName, newIndex, aliasOps, operations);
            targetOps.refresh();
            result.put("status", "SUCCESS");
            result.put("reindexedCount", reindexedCount);
            result.put("batchSize", REBUILD_BATCH_SIZE);
            result.put("activeIndex", newIndex);
            return result;
        } catch (Exception e) {
            log.error("重建帖子搜索索引失败", e);
            result.put("status", "FAILED");
            result.put("reason", e.getMessage());
            return result;
        }
    }

    public int compensateUpdatedPosts(LocalDateTime since, int batchSize) {
        if (!esEnabled || since == null || batchSize <= 0) {
            return 0;
        }
        int total = 0;
        int offset = 0;
        while (true) {
            List<SearchSyncCandidateDTO> candidates = postMapper.findUpdatedPostCandidates(since, offset, batchSize);
            if (candidates == null || candidates.isEmpty()) {
                break;
            }
            for (SearchSyncCandidateDTO candidate : candidates) {
                Long syncVersion = candidate.getUpdateTime() == null ? System.currentTimeMillis() : toEpochMillis(candidate.getUpdateTime());
                syncPostToEs(candidate.getId(), syncVersion);
                total++;
            }
            if (candidates.size() < batchSize) {
                break;
            }
            offset += candidates.size();
        }
        return total;
    }

    public SearchPostResultDTO searchPosts(String keyword, int page, int size) {
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
            log.warn("ES搜索失败，降级MySQL keyword={}", safeKeyword, e);
            return safeMysqlFallback(safeKeyword, safePage, safeSize);
        }
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "post-search");
        result.put("postFallback", "mysql+empty");
        result.put("esEnabled", esEnabled);
        result.put("indexAlias", getAliasName());

        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        PostSearchRepository repository = postSearchRepositoryProvider.getIfAvailable();
        boolean clientAvailable = operations != null;
        boolean repositoryAvailable = repository != null;
        boolean indexExists = false;
        long documentCount = 0L;
        String esStatus = "DISABLED";
        String postSearchMode = "mysql";

        if (esEnabled) {
            if (clientAvailable && repositoryAvailable) {
                try {
                    IndexOperations indexOperations = operations.indexOps(PostSearchDocument.class);
                    indexExists = indexOperations.exists();
                    if (indexExists) {
                        documentCount = repository.count();
                    }
                    esStatus = indexExists ? "UP" : "DEGRADED";
                    postSearchMode = indexExists ? "elasticsearch" : "mysql";
                } catch (Exception e) {
                    log.warn("检查ES健康状态失败", e);
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
        result.put("postSearchMode", postSearchMode);
        return result;
    }

    private SearchPostResultDTO safeMysqlFallback(String keyword, int page, int size) {
        try {
            return searchWithMysqlFallback(keyword, page, size);
        } catch (Exception e) {
            log.error("MySQL搜索失败，返回空结果 keyword={}", keyword, e);
            return emptyResult(page, size);
        }
    }

    private SearchPostResultDTO searchWithEs(String keyword, int page, int size) {
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        if (operations == null) {
            throw new IllegalStateException("ElasticsearchOperations 不可用");
        }
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("status").value(0)))
                        .should(s -> s.match(m -> m.field("title").query(keyword).boost(4.5f)))
                        .should(s -> s.match(m -> m.field("content").query(keyword).boost(2.2f)))
                        .should(s -> s.match(m -> m.field("authorName").query(keyword).boost(1.6f)))
                        .should(s -> s.match(m -> m.field("categoryName").query(keyword).boost(1.2f)))
                        .minimumShouldMatch("1")
                ))
                .withHighlightQuery(buildHighlightQuery())
                .withTrackScores(true)
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("likeCount").order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("commentCount").order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<PostSearchDocument> hits = operations.search(query, PostSearchDocument.class);
        List<SearchHit<PostSearchDocument>> hitList = hits.getSearchHits();
        List<Long> postIds = hitList.stream()
                .map(SearchHit::getContent)
                .map(PostSearchDocument::getId)
                .filter(id -> id != null)
                .toList();
        List<PostDTO> visiblePosts = postIds.isEmpty() ? List.of() : postMapper.findByIdsWithUser(postIds);
        Map<Long, PostDTO> visiblePostMap = new HashMap<>();
        for (PostDTO visiblePost : visiblePosts) {
            visiblePostMap.put(visiblePost.getId(), visiblePost);
        }
        if (visiblePostMap.size() < postIds.stream().distinct().count()) {
            List<Long> staleIds = postIds.stream()
                    .filter(id -> !visiblePostMap.containsKey(id))
                    .distinct()
                    .collect(Collectors.toList());
            log.warn("ES命中包含不可见帖子，降级MySQL查询 keyword={}, stalePostIds={}", keyword, staleIds);
            return searchWithMysqlFallback(keyword, page, size);
        }

        List<SearchPostItemDTO> list = new ArrayList<>();
        for (SearchHit<PostSearchDocument> hit : hitList) {
            PostSearchDocument doc = hit.getContent();
            PostDTO visiblePost = visiblePostMap.get(doc.getId());
            if (visiblePost == null) {
                continue;
            }
            SearchPostItemDTO item = new SearchPostItemDTO();
            item.setId(visiblePost.getId());
            item.setTitle(escapeHtml(visiblePost.getTitle()));
            item.setContent(escapeHtml(visiblePost.getContent()));
            item.setHighlightTitle(firstHighlight(hit, "title", highlight(visiblePost.getTitle(), keyword)));
            item.setHighlightContent(firstHighlight(hit, "content", buildHighlightSnippet(visiblePost.getContent(), keyword)));
            item.setAuthorName(escapeHtml(visiblePost.getAuthorName()));
            item.setCoverImage(visiblePost.getCoverImage());
            item.setCreateTime(visiblePost.getCreateTime());
            item.setLikeCount(visiblePost.getLikeCount());
            item.setCommentCount(visiblePost.getCommentCount());
            list.add(item);
        }

        SearchPostResultDTO result = new SearchPostResultDTO();
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
                .withFragmentSize(120)
                .withNoMatchSize(120)
                .build();
        Highlight highlight = new Highlight(parameters, List.of(
                new HighlightField("title"),
                new HighlightField("content")
        ));
        return new HighlightQuery(highlight, PostSearchDocument.class);
    }

    private PostSearchDocument buildDocument(PostDTO dto, Long syncVersion) {
        PostSearchDocument doc = new PostSearchDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setContent(dto.getContent());
        doc.setAuthorId(dto.getAuthorId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setCategoryId(dto.getCategoryId());
        doc.setCategoryName(dto.getCategoryName());
        doc.setCoverImage(dto.getCoverImage());
        doc.setLikeCount(dto.getLikeCount());
        doc.setCommentCount(dto.getCommentCount());
        doc.setFavoriteCount(dto.getFavoriteCount());
        doc.setViewCount(dto.getViewCount());
        doc.setStatus(dto.getStatus());
        doc.setCreateTime(dto.getCreateTime());
        doc.setUpdateTime(dto.getUpdateTime());
        doc.setSyncVersion(syncVersion == null ? System.currentTimeMillis() : syncVersion);
        return doc;
    }

    private String getAliasName() {
        ElasticsearchOperations operations = elasticsearchOperationsProvider.getIfAvailable();
        if (operations == null) {
            return "forum_post";
        }
        IndexCoordinates indexCoordinates = operations.getIndexCoordinatesFor(PostSearchDocument.class);
        return indexCoordinates == null ? "forum_post" : indexCoordinates.getIndexName();
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

    private SearchPostResultDTO searchWithMysqlFallback(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<PostDTO> posts = postMapper.findSearchPostsWithUser(keyword, offset, size);
        long total = postMapper.countSearchPosts(keyword);
        List<SearchPostItemDTO> list = new ArrayList<>();
        for (PostDTO post : posts) {
            SearchPostItemDTO item = new SearchPostItemDTO();
            item.setId(post.getId());
            item.setTitle(escapeHtml(post.getTitle()));
            item.setContent(escapeHtml(post.getContent()));
            item.setHighlightTitle(highlight(post.getTitle(), keyword));
            item.setHighlightContent(buildHighlightSnippet(post.getContent(), keyword));
            item.setAuthorName(escapeHtml(post.getAuthorName()));
            item.setCoverImage(post.getCoverImage());
            item.setCreateTime(post.getCreateTime());
            item.setLikeCount(post.getLikeCount());
            item.setCommentCount(post.getCommentCount());
            list.add(item);
        }

        SearchPostResultDTO result = new SearchPostResultDTO();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    private SearchPostResultDTO emptyResult(int page, int size) {
        SearchPostResultDTO result = new SearchPostResultDTO();
        result.setList(java.util.Collections.emptyList());
        result.setTotal(0);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    private String buildHighlightSnippet(String raw, String keyword) {
        String content = raw == null ? "" : raw;
        if (content.isEmpty()) {
            return "";
        }
        int idx = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) {
            String excerpt = content.length() > 120 ? content.substring(0, 120) + "..." : content;
            return escapeHtml(excerpt);
        }

        int start = Math.max(idx - 40, 0);
        int end = Math.min(idx + keyword.length() + 60, content.length());
        String snippet = content.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        return highlight(snippet, keyword);
    }

    private String firstHighlight(SearchHit<PostSearchDocument> hit, String field, String fallback) {
        List<String> highlighted = hit.getHighlightField(field);
        if (highlighted == null || highlighted.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return String.join("", highlighted);
    }

    private String highlight(String raw, String keyword) {
        if (raw == null || raw.isEmpty() || keyword == null || keyword.isEmpty()) {
            return raw == null ? "" : escapeHtml(raw);
        }
        String escapedRaw = escapeHtml(raw);
        String escapedKeyword = escapeHtml(keyword);
        return escapedRaw.replaceAll(
                "(?i)" + java.util.regex.Pattern.quote(escapedKeyword),
                "<em class='search-hit'>" + java.util.regex.Matcher.quoteReplacement(escapedKeyword) + "</em>"
        );
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

    private long toEpochMillis(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
