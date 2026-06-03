package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.redis.RedisSocialKeys;
import com.campus.campus_life_backend.modules.forum.entity.Comment;
import com.campus.campus_life_backend.modules.forum.entity.CommentLike;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.mapper.CommentMapper;
import com.campus.campus_life_backend.modules.forum.mapper.CommentLikeMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEvent;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEventPublisher;
import com.campus.campus_life_backend.modules.search.event.PostSearchEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final PostMapper postMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final PostSearchEventPublisher postSearchEventPublisher;
    private final SystemNotificationEventPublisher systemNotificationEventPublisher;
    private final AiContentModerationService aiContentModerationService;

    public record AdminCommentStatusChange(Integer oldStatus, Integer newStatus) {
    }

    public CommentService(
            CommentMapper commentMapper,
            CommentLikeMapper commentLikeMapper,
            PostMapper postMapper,
            ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider,
            ObjectProvider<PostSearchEventPublisher> postSearchEventPublisherProvider,
            ObjectProvider<SystemNotificationEventPublisher> systemNotificationEventPublisherProvider,
            ObjectProvider<AiContentModerationService> aiContentModerationServiceProvider) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.postSearchEventPublisher = postSearchEventPublisherProvider.getIfAvailable();
        this.systemNotificationEventPublisher = systemNotificationEventPublisherProvider.getIfAvailable();
        this.aiContentModerationService = aiContentModerationServiceProvider.getIfAvailable();
    }

    @Transactional
    @RequirePermission(anyOf = {"comment:create"})
    public Comment createComment(Long postId, Long userId, String content, Long parentId, Long replyToUserId) {
        checkCommentWithAi(userId, content);
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setStatus(0);  // 正常
        comment.setLikeCount(0);
        
        commentMapper.insertComment(comment);
        
        // 更新帖子的评论数
        postMapper.incrementCommentCount(postId);
        publishSearchUpsert(postId, "create_comment");
        publishCommentNotifications(comment);
        
        return comment;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return getCommentsByPostId(postId, null);
    }

    public List<Comment> getCommentsByPostId(Long postId, Long currentUserId) {
        List<Comment> comments = commentMapper.findByPostId(postId);
        if (comments == null || comments.isEmpty()) {
            return comments;
        }
        Set<Long> likedCommentIds = loadLikedCommentIds(currentUserId, comments);
        for (Comment comment : comments) {
            comment.setIsLiked(likedCommentIds.contains(comment.getId()));
        }
        return comments;
    }

    public long countCommentsByPostId(Long postId) {
        return commentMapper.countByPostId(postId);
    }

    public List<Comment> getCommentsByUserId(Long userId) {
        return commentMapper.findByUserId(userId);
    }

    public Comment getCommentById(Long commentId) {
        return commentMapper.findById(commentId);
    }

    @Transactional
    @RequirePermission(anyOf = {"comment:delete:self"})
    public void deleteComment(Long commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment != null) {
            commentMapper.deleteComment(commentId);
            // 减少帖子的评论数
            if (comment.getPostId() != null) {
                postMapper.decrementCommentCount(comment.getPostId());
                publishSearchUpsert(comment.getPostId(), "delete_comment");
            }
        }
    }

    @Transactional
    public AdminCommentStatusChange adminUpdateCommentStatus(Long commentId, Integer status) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_COMMENT_NOT_FOUND);
        }
        Integer oldStatus = comment.getStatus();
        comment.setStatus(status);
        commentMapper.updateComment(comment);
        if (comment.getPostId() != null) {
            publishSearchUpsert(comment.getPostId(), "admin_update_comment_status");
        }
        return new AdminCommentStatusChange(oldStatus, status);
    }

    private void publishSearchUpsert(Long postId, String reason) {
        if (postSearchEventPublisher == null || postId == null) {
            return;
        }
        try {
            postSearchEventPublisher.publishUpsert(postId, reason);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public boolean toggleLike(Long commentId, Long userId) {
        boolean currentlyLiked = isRedisMember(RedisSocialKeys.likeEntityUsers("comment", commentId), userId);
        if (!currentlyLiked) {
            CommentLike existing = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
            currentlyLiked = existing != null;
        }
        Comment comment = commentMapper.findById(commentId);
        Long ownerUserId = comment != null ? comment.getUserId() : null;

        if (currentlyLiked) {
            commentLikeMapper.deleteCommentLike(commentId, userId);
            commentMapper.decrementLikeCount(commentId);
            updateCommentLikeCache(commentId, userId, ownerUserId, false);
            return false;
        }

        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        commentLikeMapper.insertCommentLike(like);
        commentMapper.incrementLikeCount(commentId);
        updateCommentLikeCache(commentId, userId, ownerUserId, true);
        publishNotification("LIKE_COMMENT", ownerUserId, userId, comment != null ? comment.getPostId() : null, commentId, "赞了你的评论");
        return true;
    }

    private void publishCommentNotifications(Comment comment) {
        if (comment == null || comment.getPostId() == null) {
            return;
        }
        Long postOwnerId = null;
        Post post = postMapper.findById(comment.getPostId());
        if (post != null) {
            postOwnerId = post.getUserId();
        }
        publishNotification("COMMENT", postOwnerId, comment.getUserId(), comment.getPostId(), comment.getId(), "评论了你的帖子");
        if (comment.getReplyToUserId() != null && !comment.getReplyToUserId().equals(postOwnerId)) {
            publishNotification("MENTION", comment.getReplyToUserId(), comment.getUserId(), comment.getPostId(), comment.getId(), "@了你");
        }
    }

    private void publishNotification(String type, Long targetUserId, Long actorUserId, Long postId, Long commentId, String message) {
        if (systemNotificationEventPublisher == null
                || targetUserId == null
                || actorUserId == null
                || targetUserId.equals(actorUserId)) {
            return;
        }
        SystemNotificationEvent event = new SystemNotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType(type);
        event.setTargetUserId(targetUserId);
        event.setActorUserId(actorUserId);
        event.setPostId(postId);
        event.setCommentId(commentId);
        event.setMessage(message);
        event.setCreateEpochMillis(System.currentTimeMillis());
        systemNotificationEventPublisher.publish(event);
    }

    public int getCommentLikeCount(Long commentId) {
        Integer setCard = readSetCardinality(RedisSocialKeys.likeEntityUsers("comment", commentId));
        if (setCard != null) {
            cacheCount(RedisSocialKeys.commentLikeCount(commentId), setCard);
            return setCard;
        }
        Integer cached = readCachedCount(RedisSocialKeys.commentLikeCount(commentId));
        if (cached != null) {
            return cached;
        }
        Comment comment = commentMapper.findById(commentId);
        int count = comment != null && comment.getLikeCount() != null ? comment.getLikeCount() : 0;
        cacheCount(RedisSocialKeys.commentLikeCount(commentId), count);
        return count;
    }

    private boolean isCommentLikedByUser(Long commentId, Long currentUserId) {
        if (commentId == null || currentUserId == null) {
            return false;
        }
        boolean liked = isRedisMember(RedisSocialKeys.likeEntityUsers("comment", commentId), currentUserId);
        if (liked) {
            return true;
        }
        CommentLike existing = commentLikeMapper.findByCommentIdAndUserId(commentId, currentUserId);
        return existing != null;
    }

    private Set<Long> loadLikedCommentIds(Long currentUserId, List<Comment> comments) {
        Set<Long> result = new HashSet<>();
        if (currentUserId == null || comments == null || comments.isEmpty()) {
            return result;
        }

        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .filter(id -> id != null)
                .toList();
        if (commentIds.isEmpty()) {
            return result;
        }

        List<Long> likedCommentIds = commentLikeMapper.findLikedCommentIdsByUserIdAndCommentIds(currentUserId, commentIds);
        if (likedCommentIds != null) {
            result.addAll(likedCommentIds);
            return result;
        }

        for (Long commentId : commentIds) {
            if (isCommentLikedByUser(commentId, currentUserId)) {
                result.add(commentId);
            }
        }
        return result;
    }

    private void updateCommentLikeCache(Long commentId, Long userId, Long ownerUserId, boolean liked) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String usersKey = RedisSocialKeys.likeEntityUsers("comment", commentId);
            String countKey = RedisSocialKeys.commentLikeCount(commentId);
            String userLikeCountKey = ownerUserId == null ? null : RedisSocialKeys.likeUserCount(ownerUserId);
            String userValue = userId.toString();
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings({"unchecked", "NullableProblems"})
                public Object execute(RedisOperations operations) {
                    operations.multi();
                    if (liked) {
                        operations.opsForSet().add(usersKey, userValue);
                        operations.opsForValue().increment(countKey);
                        if (userLikeCountKey != null && !ownerUserId.equals(userId)) {
                            operations.opsForValue().increment(userLikeCountKey);
                        }
                    } else {
                        operations.opsForSet().remove(usersKey, userValue);
                        operations.opsForValue().decrement(countKey);
                        if (userLikeCountKey != null && !ownerUserId.equals(userId)) {
                            operations.opsForValue().decrement(userLikeCountKey);
                        }
                    }
                    return operations.exec();
                }
            });
            if (!liked) {
                normalizeCounterNonNegative(countKey);
                if (userLikeCountKey != null && !ownerUserId.equals(userId)) {
                    normalizeCounterNonNegative(userLikeCountKey);
                }
            }
        } catch (Exception e) {
            // Redis 异常不影响主流程
        }
    }

    private Integer readCachedCount(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheCount(String key, int count) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(Math.max(count, 0)));
        } catch (Exception e) {
            // Redis 异常不影响主流程
        }
    }

    private boolean isRedisMember(String key, Long userId) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean member = redisTemplate.opsForSet().isMember(key, userId.toString());
            return Boolean.TRUE.equals(member);
        } catch (Exception e) {
            return false;
        }
    }

    private Integer readSetCardinality(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Long size = redisTemplate.opsForSet().size(key);
            if (size == null) {
                return null;
            }
            return Math.toIntExact(size);
        } catch (Exception e) {
            return null;
        }
    }

    private void normalizeCounterNonNegative(String key) {
        if (redisTemplate == null || key == null) {
            return;
        }
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return;
            }
            long value = Long.parseLong(raw);
            if (value < 0) {
                redisTemplate.opsForValue().set(key, "0");
            }
        } catch (Exception ignored) {
        }
    }

    private void checkCommentWithAi(Long userId, String content) {
        if (aiContentModerationService != null) {
            aiContentModerationService.checkComment(userId, content);
        }
    }
}
