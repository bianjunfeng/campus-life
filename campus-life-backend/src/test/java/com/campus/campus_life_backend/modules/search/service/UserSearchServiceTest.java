package com.campus.campus_life_backend.modules.search.service;

import com.campus.campus_life_backend.modules.search.dto.SearchUserItemDTO;
import com.campus.campus_life_backend.modules.search.dto.UserSearchSourceDTO;
import com.campus.campus_life_backend.modules.search.es.UserSearchDocument;
import com.campus.campus_life_backend.modules.search.repository.UserSearchRepository;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSearchServiceTest {

    @Test
    void shouldRebuildUserIndexWhenEsAvailable() {
        UserMapper userMapper = mock(UserMapper.class);
        UserSearchRepository repository = mock(UserSearchRepository.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations aliasOps = mock(IndexOperations.class);
        IndexOperations targetOps = mock(IndexOperations.class);

        when(operations.getIndexCoordinatesFor(UserSearchDocument.class)).thenReturn(IndexCoordinates.of("forum_user"));
        when(operations.indexOps(UserSearchDocument.class)).thenReturn(aliasOps);
        when(operations.indexOps(any(IndexCoordinates.class))).thenReturn(targetOps);
        when(targetOps.exists()).thenReturn(false);
        when(targetOps.create(any(), any())).thenReturn(true);
        when(aliasOps.getAliases("forum_user")).thenReturn(Collections.emptyMap());
        when(userMapper.findIndexableUserIds(0, 200)).thenReturn(List.of(1L, 2L));
        when(userMapper.findIndexableUserIds(2, 200)).thenReturn(List.of());
        when(userMapper.findSearchUserById(1L)).thenReturn(buildUser(1L));
        when(userMapper.findSearchUserById(2L)).thenReturn(buildUser(2L));

        UserSearchService service = new UserSearchService(
                userMapper,
                provider(repository),
                provider(operations),
                true
        );

        Map<String, Object> result = service.rebuildUserIndex();

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(2, result.get("reindexedCount"));
        verify(aliasOps).alias(any());
        verify(operations, times(2)).save(any(UserSearchDocument.class), any(IndexCoordinates.class));
    }

    @Test
    void shouldRebuildUserIndexWhenAliasIsMissingButDirectIndexExists() {
        UserMapper userMapper = mock(UserMapper.class);
        UserSearchRepository repository = mock(UserSearchRepository.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations aliasOps = mock(IndexOperations.class);
        IndexOperations targetOps = mock(IndexOperations.class);
        IndexOperations directOps = mock(IndexOperations.class);

        when(operations.getIndexCoordinatesFor(UserSearchDocument.class)).thenReturn(IndexCoordinates.of("forum_user"));
        when(operations.indexOps(UserSearchDocument.class)).thenReturn(aliasOps);
        when(operations.indexOps(IndexCoordinates.of("forum_user"))).thenReturn(directOps);
        when(operations.indexOps(org.mockito.ArgumentMatchers.<IndexCoordinates>argThat(coords ->
                coords != null
                        && coords.getIndexName() != null
                        && coords.getIndexName().startsWith("forum_user_v")
        ))).thenReturn(targetOps);
        when(targetOps.exists()).thenReturn(false);
        when(targetOps.create(any(), any())).thenReturn(true);
        when(aliasOps.getAliases("forum_user")).thenThrow(new ResourceNotFoundException("alias [forum_user] missing"));
        when(directOps.exists()).thenReturn(true);
        when(userMapper.findIndexableUserIds(0, 200)).thenReturn(List.of(1L));
        when(userMapper.findIndexableUserIds(1, 200)).thenReturn(List.of());
        when(userMapper.findSearchUserById(1L)).thenReturn(buildUser(1L));

        UserSearchService service = new UserSearchService(
                userMapper,
                provider(repository),
                provider(operations),
                true
        );

        Map<String, Object> result = service.rebuildUserIndex();

        assertEquals("SUCCESS", result.get("status"));
        verify(aliasOps).alias(any());
    }

    @Test
    void shouldFallbackToMysqlWhenUserEsDisabled() {
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.searchUsers("alice", 0, 8)).thenReturn(List.of(buildSearchItem(1L)));
        when(userMapper.countSearchUsers("alice")).thenReturn(1L);

        UserSearchService service = new UserSearchService(
                userMapper,
                provider(null),
                provider(null),
                false
        );

        var result = service.searchUsers("alice", 1, 8);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getList().get(0).getId());
    }

    @Test
    void shouldReportMysqlModeWhenEsUnavailable() {
        UserSearchService service = new UserSearchService(
                mock(UserMapper.class),
                provider(null),
                provider(null),
                false
        );

        Map<String, Object> result = service.getHealthStatus();

        assertEquals("DISABLED", result.get("esStatus"));
        assertEquals("mysql", result.get("searchMode"));
        assertFalse((Boolean) result.get("esEnabled"));
    }

    private static UserSearchSourceDTO buildUser(Long id) {
        UserSearchSourceDTO dto = new UserSearchSourceDTO();
        dto.setId(id);
        dto.setUsername("user-" + id);
        dto.setStatus(1);
        dto.setRole(0);
        dto.setBio("bio");
        dto.setPhone("13800000000");
        dto.setPostCount(3);
        dto.setFollowerCount(4);
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        return dto;
    }

    private static SearchUserItemDTO buildSearchItem(Long id) {
        SearchUserItemDTO item = new SearchUserItemDTO();
        item.setId(id);
        item.setUsername("alice");
        return item;
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
