package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.forum.dto.PostDTO;
import com.campus.campus_life_backend.modules.forum.entity.Comment;
import com.campus.campus_life_backend.modules.forum.entity.CommentLike;
import com.campus.campus_life_backend.modules.forum.entity.PostLike;
import com.campus.campus_life_backend.modules.forum.mapper.CommentLikeMapper;
import com.campus.campus_life_backend.modules.forum.mapper.CommentMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostFavoriteMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostImageMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostLikeMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.forum.service.CommentService;
import com.campus.campus_life_backend.modules.forum.service.PostService;
import com.campus.campus_life_backend.modules.user.service.UserFollowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostInteractionStateTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private PostImageMapper postImageMapper;
    @Mock
    private PostFavoriteMapper postFavoriteMapper;
    @Mock
    private PostLikeMapper postLikeMapper;
    @Mock
    private UserFollowService userFollowService;
    @Mock
    private ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;
    @Mock
    private ObjectProvider<com.campus.campus_life_backend.modules.search.event.PostSearchEventPublisher> postSearchEventPublisherProvider;
    @Mock
    private ObjectProvider<com.campus.campus_life_backend.modules.message.event.SystemNotificationEventPublisher> systemNotificationEventPublisherProvider;
    @Mock
    private ObjectProvider<com.campus.campus_life_backend.modules.forum.service.SensitiveFilterService> sensitiveFilterServiceProvider;

    @InjectMocks
    private PostService postService;

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentLikeMapper commentLikeMapper;
    @Mock
    private ObjectProvider<com.campus.campus_life_backend.modules.search.event.PostSearchEventPublisher> commentSearchEventPublisherProvider;
    @Mock
    private ObjectProvider<com.campus.campus_life_backend.modules.message.event.SystemNotificationEventPublisher> commentNotificationEventPublisherProvider;

    @InjectMocks
    private CommentService commentService;

    @Test
    void shouldPopulatePostLikedStateForCurrentUser() {
        PostDTO dto = new PostDTO();
        dto.setId(10L);
        dto.setAuthorId(8L);

        PostLike like = new PostLike();
        like.setPostId(10L);
        like.setUserId(2L);

        when(postMapper.findLatestPostsWithUser(0, 10)).thenReturn(List.of(dto));
        when(postImageMapper.findByPostIds(List.of(10L))).thenReturn(java.util.Collections.emptyList());
        when(postLikeMapper.findLikedPostIdsByUserIdAndPostIds(2L, List.of(10L))).thenReturn(List.<Long>of(10L));
        when(postFavoriteMapper.findFavoritedPostIdsByUserIdAndPostIds(2L, List.of(10L))).thenReturn(java.util.Collections.emptyList());
        when(userFollowService.findFollowedAuthorIds(2L, List.of(8L))).thenReturn(Set.of(8L));

        List<PostDTO> posts = postService.getPostsWithUserByOrder("latest", 1, 10, null, 2L);

        assertTrue(Boolean.TRUE.equals(posts.get(0).getIsLiked()));
        assertTrue(Boolean.TRUE.equals(posts.get(0).getIsFollowed()));
        assertFalse(Boolean.TRUE.equals(posts.get(0).getIsCollected()));
    }

    @Test
    void shouldPopulateCommentLikedStateForCurrentUser() {
        Comment comment = new Comment();
        comment.setId(12L);

        CommentLike like = new CommentLike();
        like.setCommentId(12L);
        like.setUserId(2L);

        when(commentMapper.findByPostId(5L)).thenReturn(List.of(comment));
        when(commentLikeMapper.findLikedCommentIdsByUserIdAndCommentIds(2L, List.of(12L))).thenReturn(List.<Long>of(12L));

        List<Comment> comments = commentService.getCommentsByPostId(5L, 2L);

        assertTrue(Boolean.TRUE.equals(comments.get(0).getIsLiked()));
        verify(commentLikeMapper).findLikedCommentIdsByUserIdAndCommentIds(2L, List.of(12L));
    }

    @Test
    void shouldDefaultCommentLikedStateToFalseWhenAnonymous() {
        Comment comment = new Comment();
        comment.setId(12L);

        when(commentMapper.findByPostId(5L)).thenReturn(List.of(comment));

        List<Comment> comments = commentService.getCommentsByPostId(5L, null);

        assertFalse(Boolean.TRUE.equals(comments.get(0).getIsLiked()));
        verify(commentLikeMapper, never()).findByCommentIdAndUserId(12L, null);
    }
}
