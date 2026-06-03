package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.redis.RedisSocialKeys;
import com.campus.campus_life_backend.modules.forum.dto.PostDTO;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.entity.PostImage;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostImageMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostFavoriteMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostLikeMapper;
import com.campus.campus_life_backend.modules.forum.entity.PostFavorite;
import com.campus.campus_life_backend.modules.forum.entity.PostLike;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEvent;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEventPublisher;
import com.campus.campus_life_backend.modules.search.event.PostSearchEventPublisher;
import com.campus.campus_life_backend.modules.user.service.UserFollowService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class PostService {

    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserFollowService userFollowService;
    private final RedisTemplate<String, String> redisTemplate;
    private final PostSearchEventPublisher postSearchEventPublisher;
    private final SystemNotificationEventPublisher systemNotificationEventPublisher;
    private final SensitiveFilterService sensitiveFilterService;
    private final AiContentModerationService aiContentModerationService;

    public record AdminPostStatusChange(Integer oldStatus, Integer newStatus) {
    }

    public PostService(PostMapper postMapper, PostImageMapper postImageMapper, PostFavoriteMapper postFavoriteMapper,
                       PostLikeMapper postLikeMapper, UserFollowService userFollowService,
                       ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider,
                       ObjectProvider<PostSearchEventPublisher> postSearchEventPublisherProvider,
                       ObjectProvider<SystemNotificationEventPublisher> systemNotificationEventPublisherProvider,
                       ObjectProvider<SensitiveFilterService> sensitiveFilterServiceProvider,
                       ObjectProvider<AiContentModerationService> aiContentModerationServiceProvider) {
        this.postMapper = postMapper;
        this.postImageMapper = postImageMapper;
        this.postFavoriteMapper = postFavoriteMapper;
        this.postLikeMapper = postLikeMapper;
        this.userFollowService = userFollowService;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.postSearchEventPublisher = postSearchEventPublisherProvider.getIfAvailable();
        this.systemNotificationEventPublisher = systemNotificationEventPublisherProvider.getIfAvailable();
        this.sensitiveFilterService = sensitiveFilterServiceProvider.getIfAvailable();
        this.aiContentModerationService = aiContentModerationServiceProvider.getIfAvailable();
    }

    @Transactional
    @RequirePermission(anyOf = {"post:create"})
    public Post createPost(Long userId, String title, String content, Long categoryId) {
        return createPost(userId, title, content, categoryId, null);
    }
    
    @Transactional
    @RequirePermission(anyOf = {"post:create"})
    public Post createPost(Long userId, String title, String content, Long categoryId, List<PostImageInfo> images) {
        checkPostWithAi(userId, title, content);
        String safeTitle = sanitizeText(title);
        String safeContent = sanitizeText(content);
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(safeTitle);
        post.setContent(safeContent);
        post.setCategoryId(categoryId);
        post.setStatus(0);  // 正常
        post.setIsPinned(0);
        post.setIsHot(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setViewCount(0);
        
        postMapper.insertPost(post);
        
        // 如果有图片，插入图片记录
        if (images != null && !images.isEmpty()) {
            // 第一张图片的sort_order设置为最高（作为首图）
            // 后续图片的sort_order递减
            int maxSortOrder = images.size();
            for (int i = 0; i < images.size(); i++) {
                PostImageInfo imgInfo = images.get(i);
                PostImage postImage = new PostImage();
                postImage.setPostId(post.getId());
                postImage.setUrl(imgInfo.getUrl());
                postImage.setWidth(imgInfo.getWidth());
                postImage.setHeight(imgInfo.getHeight());
                // 第一张图片（索引0）的sort_order最大，作为首图
                postImage.setSortOrder(maxSortOrder - i);
                postImageMapper.insertPostImage(postImage);
            }
        }
        publishSearchUpsert(post.getId(), "create_post");
        return post;
    }
    
    /**
     * 图片信息内部类
     */
    public static class PostImageInfo {
        private String url;
        private Integer width;
        private Integer height;
        
        public PostImageInfo() {}
        
        public PostImageInfo(String url, Integer width, Integer height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public Integer getWidth() {
            return width;
        }
        
        public void setWidth(Integer width) {
            this.width = width;
        }
        
        public Integer getHeight() {
            return height;
        }
        
        public void setHeight(Integer height) {
            this.height = height;
        }
    }

    public Post getPostById(Long id) {
        Post post = postMapper.findById(id);
        if (post != null && post.getStatus() == 0) {
            // 增加浏览量
            postMapper.incrementViewCount(id);
            post.setViewCount(post.getViewCount() + 1);
            publishSearchUpsert(id, "view_post");
        }
        return post;
    }

    /**
     * 获取帖子详情（包含用户信息和图片列表）
     * @param id 帖子ID
     * @param currentUserId 当前登录用户ID（可为null，如果未登录）
     */
    public PostDTO getPostDetailById(Long id, Long currentUserId) {
        PostDTO postDTO = postMapper.findByIdWithUser(id);
        if (postDTO == null) {
            return null;
        }
        
        // 增加浏览量
        postMapper.incrementViewCount(id);
        postDTO.setViewCount(postDTO.getViewCount() + 1);
        publishSearchUpsert(id, "view_post_detail");
        
        // 查询帖子图片
        List<PostImage> images = postImageMapper.findByPostId(id);
        if (images != null && !images.isEmpty()) {
            postDTO.setImages(images.stream().map(img -> {
                PostDTO.PostImageDTO dto = new PostDTO.PostImageDTO();
                dto.setId(img.getId());
                dto.setUrl(img.getUrl());
                dto.setWidth(img.getWidth());
                dto.setHeight(img.getHeight());
                return dto;
            }).collect(Collectors.toList()));
        }
        
        // 如果用户已登录，检查是否已关注作者
        if (currentUserId != null && postDTO.getAuthorId() != null && !currentUserId.equals(postDTO.getAuthorId())) {
            boolean isFollowed = userFollowService.isFollowing(currentUserId, postDTO.getAuthorId());
            postDTO.setIsFollowed(isFollowed);
        } else {
            postDTO.setIsFollowed(false);
        }

        postDTO.setIsLiked(isPostLikedByUser(id, currentUserId));
        
        // 如果用户已登录，检查是否已收藏
        if (currentUserId != null) {
            PostFavorite favorite = postFavoriteMapper.findByPostIdAndUserId(id, currentUserId);
            postDTO.setIsCollected(favorite != null);
        } else {
            postDTO.setIsCollected(false);
        }
        
        return postDTO;
    }

    public List<Post> getPostsByOrder(String order, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        int offset = (safePage - 1) * safeSize;
        if ("hot".equals(order)) {
            return postMapper.findHotPosts(offset, safeSize);
        } else {
            return postMapper.findLatestPosts(offset, safeSize);
        }
    }

    /**
     * 获取帖子列表（带用户信息）
     */
    public List<PostDTO> getPostsWithUserByOrder(String order, Integer page, Integer size, Long categoryId, Long currentUserId) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        int offset = (safePage - 1) * safeSize;

        // 如果指定了分类ID，按分类筛选
        if (categoryId != null) {
            List<PostDTO> result = postMapper.findByCategoryIdWithUser(categoryId, offset, safeSize);
            return enrichPosts(result, currentUserId, false);
        }
        
        // 否则按排序方式查询
        if ("hot".equals(order)) {
            return enrichPosts(postMapper.findHotPostsWithUser(offset, safeSize), currentUserId, false);
        } else if ("follow".equals(order)) {
            if (currentUserId == null) {
                return new ArrayList<>();
            }
            return enrichPosts(postMapper.findFollowPostsWithUser(currentUserId, offset, safeSize), currentUserId, false);
        } else {
            return enrichPosts(postMapper.findLatestPostsWithUser(offset, safeSize), currentUserId, false);
        }
    }

    public List<PostDTO> getFollowPostsWithUser(Long currentUserId, Integer page, Integer size) {
        if (currentUserId == null) {
            return new ArrayList<>();
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        int offset = (safePage - 1) * safeSize;
        return enrichPosts(postMapper.findFollowPostsWithUser(currentUserId, offset, safeSize), currentUserId, false);
    }

    public long countPostsByOrder(String order, Long categoryId, Long currentUserId) {
        if (categoryId != null) {
            return postMapper.countByCategoryId(categoryId);
        }
        if ("hot".equals(order)) {
            return postMapper.countHotPosts();
        }
        if ("follow".equals(order)) {
            if (currentUserId == null) {
                return 0;
            }
            return postMapper.countFollowPostsWithUser(currentUserId);
        }
        return postMapper.countLatestPosts();
    }

    public List<Post> getPostsByUserId(Long userId) {
        return postMapper.findByUserId(userId);
    }

    public List<PostDTO> getPostsByUserId(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        // 使用带用户信息的查询方法
        List<PostDTO> posts = postMapper.findByUserIdWithUserAndPagination(userId, offset, size);
        return enrichPosts(posts, null, false);
    }

    public List<Post> getPostsByCategoryId(Long categoryId) {
        return postMapper.findByCategoryId(categoryId);
    }

    @Transactional
    @RequirePermission(anyOf = {"post:update:self"})
    public Post updatePost(Long id, String title, String content, Long categoryId) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }
        if (title != null || content != null) {
            checkPostWithAi(post.getUserId(), title == null ? post.getTitle() : title, content == null ? post.getContent() : content);
        }
        if (title != null) {
            post.setTitle(sanitizeText(title));
        }
        if (content != null) {
            post.setContent(sanitizeText(content));
        }
        if (categoryId != null) {
            post.setCategoryId(categoryId);
        }
        postMapper.updatePost(post);
        publishSearchUpsert(post.getId(), "update_post");
        return post;
    }

    @Transactional
    @RequirePermission(anyOf = {"post:update:self"})
    public Post updatePostStatus(Long id, Integer status) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }
        post.setStatus(status);
        postMapper.updatePost(post);
        if (status != null && status == 0) {
            publishSearchUpsert(post.getId(), "update_post_status");
        } else {
            publishSearchDelete(post.getId(), "hide_or_delete_post");
        }
        return post;
    }

    @Transactional
    public AdminPostStatusChange adminUpdatePostStatus(Long id, Integer status, String reason) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }
        Integer oldStatus = post.getStatus();
        post.setStatus(status);
        postMapper.updatePost(post);
        if (status != null && status == 0) {
            publishSearchUpsert(post.getId(), reason);
        } else {
            publishSearchDelete(post.getId(), reason);
        }
        return new AdminPostStatusChange(oldStatus, status);
    }

    @Transactional
    public void adminSetPinned(Long id, boolean pinned) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }
        post.setIsPinned(pinned ? 1 : 0);
        postMapper.updatePost(post);
        publishSearchUpsert(post.getId(), pinned ? "admin_pin_post" : "admin_unpin_post");
    }

    @Transactional
    public void adminSetHot(Long id, boolean hot) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }
        post.setIsHot(hot ? 1 : 0);
        postMapper.updatePost(post);
        publishSearchUpsert(post.getId(), hot ? "admin_set_hot_post" : "admin_remove_hot_post");
    }

    @Transactional
    @RequirePermission(anyOf = {"post:delete:self"})
    public void deletePost(Long id) {
        postMapper.deletePost(id);
        publishSearchDelete(id, "delete_post");
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

    private void publishSearchDelete(Long postId, String reason) {
        if (postSearchEventPublisher == null || postId == null) {
            return;
        }
        try {
            postSearchEventPublisher.publishDelete(postId, reason);
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        boolean currentlyLiked = isRedisMember(RedisSocialKeys.likeEntityUsers("post", postId), userId);
        if (!currentlyLiked) {
            PostLike existing = postLikeMapper.findByPostIdAndUserId(postId, userId);
            currentlyLiked = existing != null;
        }
        boolean nextLiked = !currentlyLiked;
        Post post = postMapper.findById(postId);
        Long ownerUserId = post != null ? post.getUserId() : null;

        if (currentlyLiked) {
            postLikeMapper.deletePostLike(postId, userId);
            postMapper.decrementLikeCount(postId);
            updateLikeCache(postId, userId, ownerUserId, false);
            publishSearchUpsert(postId, "unlike_post");
            return false;
        }

        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        postLikeMapper.insertPostLike(like);
        postMapper.incrementLikeCount(postId);
        updateLikeCache(postId, userId, ownerUserId, nextLiked);
        publishNotification("LIKE", ownerUserId, userId, postId, null, "赞了你的帖子");
        publishSearchUpsert(postId, "like_post");
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        boolean currentlyFavorited = isRedisMember(RedisSocialKeys.postFavoriteUsers(postId), userId);
        if (!currentlyFavorited) {
            PostFavorite existing = postFavoriteMapper.findByPostIdAndUserId(postId, userId);
            currentlyFavorited = existing != null;
        }
        if (currentlyFavorited) {
            // 已收藏，取消收藏
            postFavoriteMapper.deletePostFavorite(postId, userId);
            postMapper.decrementFavoriteCount(postId);
            updateFavoriteCache(postId, userId, false);
            publishSearchUpsert(postId, "unfavorite_post");
            return false;
        } else {
            // 未收藏，添加收藏
            PostFavorite favorite = new PostFavorite();
            favorite.setPostId(postId);
            favorite.setUserId(userId);
            favorite.setCreateTime(LocalDateTime.now());
            postFavoriteMapper.insertPostFavorite(favorite);
            postMapper.incrementFavoriteCount(postId);
            updateFavoriteCache(postId, userId, true);
            Post post = postMapper.findById(postId);
            Long ownerUserId = post != null ? post.getUserId() : null;
            publishNotification("FAVORITE", ownerUserId, userId, postId, null, "收藏了你的帖子");
            publishSearchUpsert(postId, "favorite_post");
            return true;
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

    public int getPostLikeCount(Long postId) {
        Integer setCard = readSetCardinality(RedisSocialKeys.likeEntityUsers("post", postId));
        if (setCard != null) {
            cacheCount(RedisSocialKeys.postLikeCount(postId), setCard);
            return setCard;
        }
        Integer cached = readCachedCount(RedisSocialKeys.postLikeCount(postId));
        if (cached != null) {
            return cached;
        }
        Post post = postMapper.findById(postId);
        int count = post != null && post.getLikeCount() != null ? post.getLikeCount() : 0;
        cacheCount(RedisSocialKeys.postLikeCount(postId), count);
        return count;
    }

    public int getPostFavoriteCount(Long postId) {
        Integer cached = readCachedCount(RedisSocialKeys.postFavoriteCount(postId));
        if (cached != null) {
            return cached;
        }
        Post post = postMapper.findById(postId);
        int count = post != null && post.getFavoriteCount() != null ? post.getFavoriteCount() : 0;
        cacheCount(RedisSocialKeys.postFavoriteCount(postId), count);
        return count;
    }

    private void updateLikeCache(Long postId, Long userId, Long ownerUserId, boolean liked) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String usersKey = RedisSocialKeys.likeEntityUsers("post", postId);
            String countKey = RedisSocialKeys.postLikeCount(postId);
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

    private void updateFavoriteCache(Long postId, Long userId, boolean favorited) {
        if (redisTemplate == null) {
            return;
        }
        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            String usersKey = RedisSocialKeys.postFavoriteUsers(postId);
            String countKey = RedisSocialKeys.postFavoriteCount(postId);
            String userValue = userId.toString();
            if (favorited) {
                setOps.add(usersKey, userValue);
                redisTemplate.opsForValue().increment(countKey);
            } else {
                setOps.remove(usersKey, userValue);
                Long remain = redisTemplate.opsForValue().decrement(countKey);
                if (remain != null && remain < 0) {
                    redisTemplate.opsForValue().set(countKey, "0");
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

    public List<PostDTO> getFavoritePostsByUserId(Long userId, Integer page, Integer size) {
        return getFavoritePostsByUserId(userId, userId, page, size);
    }

    public List<PostDTO> getFavoritePostsByUserId(Long ownerUserId, Long currentUserId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Long> postIds = postFavoriteMapper.findPostIdsByUserIdWithPagination(ownerUserId, offset, size);
        if (postIds == null) {
            postIds = loadFavoritePostIdsInMemory(ownerUserId, offset, size);
        }
        if (postIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<PostDTO> posts = postMapper.findByIdsWithUser(postIds);
        Map<Long, PostDTO> postMap = new HashMap<>();
        if (posts != null) {
            for (PostDTO post : posts) {
                postMap.put(post.getId(), post);
            }
        }

        List<PostDTO> orderedPosts = new ArrayList<>();
        for (Long postId : postIds) {
            PostDTO post = postMap.get(postId);
            if (post != null) {
                orderedPosts.add(post);
            }
        }

        return enrichPosts(orderedPosts, currentUserId, ownerUserId != null && ownerUserId.equals(currentUserId));
    }

    private List<PostDTO> enrichPosts(List<PostDTO> posts, Long currentUserId, boolean markCollected) {
        if (posts == null || posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostDTO::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, List<PostImage>> imagesByPostId = loadImagesByPostIds(postIds);
        Set<Long> likedPostIds = loadLikedPostIds(currentUserId, postIds);
        Set<Long> collectedPostIds = markCollected ? new HashSet<>(postIds) : loadCollectedPostIds(currentUserId, postIds);
        Set<Long> authorIds = posts.stream()
                .map(PostDTO::getAuthorId)
                .filter(id -> id != null && !id.equals(currentUserId))
                .collect(Collectors.toSet());
        Set<Long> followedAuthorIds = loadFollowedAuthorIds(currentUserId, authorIds);

        for (PostDTO dto : posts) {
            List<PostImage> images = imagesByPostId.get(dto.getId());
            if (images != null && !images.isEmpty()) {
                dto.setImages(images.stream().map(img -> {
                    PostDTO.PostImageDTO imgDto = new PostDTO.PostImageDTO();
                    imgDto.setId(img.getId());
                    imgDto.setUrl(img.getUrl());
                    imgDto.setWidth(img.getWidth());
                    imgDto.setHeight(img.getHeight());
                    return imgDto;
                }).collect(Collectors.toList()));
            }

            dto.setIsLiked(likedPostIds.contains(dto.getId()));

            if (currentUserId != null && dto.getAuthorId() != null && !currentUserId.equals(dto.getAuthorId())) {
                dto.setIsFollowed(followedAuthorIds.contains(dto.getAuthorId()));
            } else {
                dto.setIsFollowed(false);
            }

            if (markCollected) {
                dto.setIsCollected(true);
            } else {
                dto.setIsCollected(collectedPostIds.contains(dto.getId()));
            }
        }

        return posts;
    }

    private List<Long> loadFavoritePostIdsInMemory(Long ownerUserId, int offset, int size) {
        List<PostFavorite> favorites = postFavoriteMapper.findByUserId(ownerUserId);
        if (favorites == null || favorites.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> allPostIds = favorites.stream()
                .map(PostFavorite::getPostId)
                .collect(Collectors.toList());
        int total = allPostIds.size();
        int fromIndex = Math.min(offset, total);
        int toIndex = Math.min(offset + size, total);
        if (fromIndex >= total) {
            return new ArrayList<>();
        }
        return new ArrayList<>(allPostIds.subList(fromIndex, toIndex));
    }

    private Map<Long, List<PostImage>> loadImagesByPostIds(List<Long> postIds) {
        Map<Long, List<PostImage>> result = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return result;
        }

        List<PostImage> images = postImageMapper.findByPostIds(postIds);
        if (images != null) {
            for (PostImage image : images) {
                if (image.getPostId() == null) {
                    continue;
                }
                result.computeIfAbsent(image.getPostId(), key -> new ArrayList<>()).add(image);
            }
            return result;
        }

        for (Long postId : postIds) {
            List<PostImage> postImages = postImageMapper.findByPostId(postId);
            if (postImages != null && !postImages.isEmpty()) {
                result.put(postId, postImages);
            }
        }
        return result;
    }

    private Set<Long> loadLikedPostIds(Long currentUserId, List<Long> postIds) {
        Set<Long> result = new HashSet<>();
        if (currentUserId == null || postIds == null || postIds.isEmpty()) {
            return result;
        }

        List<Long> likedPostIds = postLikeMapper.findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);
        if (likedPostIds != null) {
            result.addAll(likedPostIds);
            return result;
        }

        for (Long postId : postIds) {
            if (isPostLikedByUser(postId, currentUserId)) {
                result.add(postId);
            }
        }
        return result;
    }

    private Set<Long> loadCollectedPostIds(Long currentUserId, List<Long> postIds) {
        Set<Long> result = new HashSet<>();
        if (currentUserId == null || postIds == null || postIds.isEmpty()) {
            return result;
        }

        List<Long> collectedPostIds = postFavoriteMapper.findFavoritedPostIdsByUserIdAndPostIds(currentUserId, postIds);
        if (collectedPostIds != null) {
            result.addAll(collectedPostIds);
            return result;
        }

        for (Long postId : postIds) {
            if (postFavoriteMapper.findByPostIdAndUserId(postId, currentUserId) != null) {
                result.add(postId);
            }
        }
        return result;
    }

    private Set<Long> loadFollowedAuthorIds(Long currentUserId, Set<Long> authorIds) {
        Set<Long> result = new HashSet<>();
        if (currentUserId == null || authorIds == null || authorIds.isEmpty()) {
            return result;
        }

        Set<Long> followedAuthorIds = userFollowService.findFollowedAuthorIds(currentUserId, new ArrayList<>(authorIds));
        if (followedAuthorIds != null) {
            result.addAll(followedAuthorIds);
            return result;
        }

        for (Long authorId : authorIds) {
            if (userFollowService.isFollowing(currentUserId, authorId)) {
                result.add(authorId);
            }
        }
        return result;
    }

    private boolean isPostLikedByUser(Long postId, Long currentUserId) {
        if (postId == null || currentUserId == null) {
            return false;
        }
        boolean liked = isRedisMember(RedisSocialKeys.likeEntityUsers("post", postId), currentUserId);
        if (liked) {
            return true;
        }
        PostLike existing = postLikeMapper.findByPostIdAndUserId(postId, currentUserId);
        return existing != null;
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

    private String sanitizeText(String raw) {
        if (raw == null) {
            return "";
        }
        String escaped = HtmlUtils.htmlEscape(raw.trim());
        if (escaped.isEmpty()) {
            return escaped;
        }
        if (sensitiveFilterService == null) {
            return escaped;
        }
        return sensitiveFilterService.filter(escaped);
    }

    private void checkPostWithAi(Long userId, String title, String content) {
        if (aiContentModerationService != null) {
            aiContentModerationService.checkPost(userId, title, content);
        }
    }
}
