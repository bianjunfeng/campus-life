package com.campus.campus_life_backend.modules.search.service;

import com.campus.campus_life_backend.modules.forum.dto.PostDTO;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.search.es.PostSearchDocument;
import com.campus.campus_life_backend.modules.search.repository.PostSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostSearchServiceTest {

    @Test
    void shouldRebuildPostIndexWhenEsAvailable() {
        PostMapper postMapper = mock(PostMapper.class);
        PostSearchRepository repository = mock(PostSearchRepository.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations aliasOps = mock(IndexOperations.class);
        IndexOperations targetOps = mock(IndexOperations.class);

        when(operations.getIndexCoordinatesFor(PostSearchDocument.class)).thenReturn(IndexCoordinates.of("forum_post"));
        when(operations.indexOps(PostSearchDocument.class)).thenReturn(aliasOps);
        when(operations.indexOps(any(IndexCoordinates.class))).thenReturn(targetOps);
        when(targetOps.exists()).thenReturn(false);
        when(targetOps.create(any(), any())).thenReturn(true);
        when(aliasOps.getAliases("forum_post")).thenReturn(Collections.emptyMap());
        when(postMapper.findIndexablePostIds(0, 200)).thenReturn(List.of(1L, 2L));
        when(postMapper.findIndexablePostIds(2, 200)).thenReturn(List.of());
        when(postMapper.findByIdWithUser(1L)).thenReturn(buildPost(1L));
        when(postMapper.findByIdWithUser(2L)).thenReturn(buildPost(2L));

        PostSearchService service = new PostSearchService(
                postMapper,
                provider(repository),
                provider(operations),
                true
        );

        Map<String, Object> result = service.rebuildPostIndex();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(2, result.get("reindexedCount"));
        verify(targetOps).create(any(), any());
        verify(aliasOps).alias(any());
        verify(operations, times(2)).save(any(PostSearchDocument.class), any(IndexCoordinates.class));
    }

    @Test
    void shouldRebuildPostIndexWhenAliasIsMissingButDirectIndexExists() {
        PostMapper postMapper = mock(PostMapper.class);
        PostSearchRepository repository = mock(PostSearchRepository.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations aliasOps = mock(IndexOperations.class);
        IndexOperations targetOps = mock(IndexOperations.class);
        IndexOperations directOps = mock(IndexOperations.class);

        when(operations.getIndexCoordinatesFor(PostSearchDocument.class)).thenReturn(IndexCoordinates.of("forum_post"));
        when(operations.indexOps(PostSearchDocument.class)).thenReturn(aliasOps);
        when(operations.indexOps(IndexCoordinates.of("forum_post"))).thenReturn(directOps);
        when(operations.indexOps(org.mockito.ArgumentMatchers.<IndexCoordinates>argThat(coords ->
                coords != null
                        && coords.getIndexName() != null
                        && coords.getIndexName().startsWith("forum_post_v")
        ))).thenReturn(targetOps);
        when(targetOps.exists()).thenReturn(false);
        when(targetOps.create(any(), any())).thenReturn(true);
        when(aliasOps.getAliases("forum_post")).thenThrow(new ResourceNotFoundException("alias [forum_post] missing"));
        when(directOps.exists()).thenReturn(true);
        when(postMapper.findIndexablePostIds(0, 200)).thenReturn(List.of(1L));
        when(postMapper.findIndexablePostIds(1, 200)).thenReturn(List.of());
        when(postMapper.findByIdWithUser(1L)).thenReturn(buildPost(1L));

        PostSearchService service = new PostSearchService(
                postMapper,
                provider(repository),
                provider(operations),
                true
        );

        Map<String, Object> result = service.rebuildPostIndex();

        assertEquals("SUCCESS", result.get("status"));
        verify(aliasOps).alias(any());
    }

    @Test
    void shouldReturnMysqlModeWhenEsDisabled() {
        PostSearchService service = new PostSearchService(
                mock(PostMapper.class),
                provider(null),
                provider(null),
                false
        );

        Map<String, Object> result = service.getHealthStatus();

        assertEquals("DISABLED", result.get("esStatus"));
        assertEquals("mysql", result.get("postSearchMode"));
        assertFalse((Boolean) result.get("esEnabled"));
    }

    @Test
    void shouldReportDegradedWhenEsIndexMissing() {
        PostSearchRepository repository = mock(PostSearchRepository.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(operations.getIndexCoordinatesFor(PostSearchDocument.class)).thenReturn(IndexCoordinates.of("forum_post"));
        when(operations.indexOps(PostSearchDocument.class)).thenReturn(indexOperations);
        when(indexOperations.exists()).thenReturn(false);

        PostSearchService service = new PostSearchService(
                mock(PostMapper.class),
                provider(repository),
                provider(operations),
                true
        );

        Map<String, Object> result = service.getHealthStatus();

        assertEquals("DEGRADED", result.get("esStatus"));
        assertEquals("mysql", result.get("postSearchMode"));
        assertFalse((Boolean) result.get("indexExists"));
    }

    @Test
    void shouldSkipRebuildWhenEsUnavailable() {
        PostMapper postMapper = mock(PostMapper.class);
        PostSearchService service = new PostSearchService(
                postMapper,
                provider(null),
                provider(null),
                true
        );

        Map<String, Object> result = service.rebuildPostIndex();

        assertEquals("SKIPPED", result.get("status"));
        verify(postMapper, never()).findIndexablePostIds(0, 200);
    }

    @Test
    void shouldFallbackToMysqlWhenEsContainsInvisiblePosts() {
        PostMapper postMapper = mock(PostMapper.class);
        PostSearchRepository repository = mock(PostSearchRepository.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        @SuppressWarnings("unchecked")
        SearchHits<PostSearchDocument> hits = mock(SearchHits.class);
        @SuppressWarnings("unchecked")
        SearchHit<PostSearchDocument> hit = mock(SearchHit.class);

        PostSearchDocument doc = new PostSearchDocument();
        doc.setId(1L);
        doc.setTitle("es-title");
        doc.setContent("es-content");

        when(operations.search(any(Query.class), eq(PostSearchDocument.class))).thenReturn(hits);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hits.getTotalHits()).thenReturn(1L);
        when(hit.getContent()).thenReturn(doc);
        when(postMapper.findByIdsWithUser(List.of(1L))).thenReturn(List.of());
        when(postMapper.findSearchPostsWithUser("java", 0, 10)).thenReturn(List.of(buildPost(2L)));
        when(postMapper.countSearchPosts("java")).thenReturn(1L);

        PostSearchService service = new PostSearchService(
                postMapper,
                provider(repository),
                provider(operations),
                true
        );

        var result = service.searchPosts("java", 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(2L, result.getList().get(0).getId());
        verify(postMapper).findSearchPostsWithUser("java", 0, 10);
        verify(postMapper).countSearchPosts("java");
    }

    private static PostDTO buildPost(Long id) {
        PostDTO dto = new PostDTO();
        dto.setId(id);
        dto.setStatus(0);
        dto.setTitle("title-" + id);
        dto.setContent("content-" + id);
        dto.setAuthorId(10L);
        dto.setAuthorName("author");
        dto.setCategoryId(20L);
        dto.setCategoryName("category");
        return dto;
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }

            @Override
            public Stream<T> stream() {
                return value == null ? Stream.empty() : Stream.of(value);
            }

            @Override
            public Stream<T> orderedStream() {
                return stream();
            }
        };
    }

}
