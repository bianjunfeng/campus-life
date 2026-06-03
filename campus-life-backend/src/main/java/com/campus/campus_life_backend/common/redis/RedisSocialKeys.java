package com.campus.campus_life_backend.common.redis;

/**
 * 社交互动相关 Redis Key 约定。
 */
public final class RedisSocialKeys {

    private static final String PREFIX = "social:";
    private static final String LIKE_PREFIX = "like:";
    private static final String FOLLOWEE_PREFIX = "followee:";
    private static final String FOLLOWER_PREFIX = "follower:";

    private RedisSocialKeys() {
    }

    /**
     * 某实体的点赞用户集合：
     * like:entity:{entityType}:{entityId} -> set(userId)
     */
    public static String likeEntityUsers(String entityType, Long entityId) {
        return LIKE_PREFIX + "entity:" + entityType + ":" + entityId;
    }

    /**
     * 某用户收到的点赞数：
     * like:user:{userId} -> int
     */
    public static String likeUserCount(Long userId) {
        return LIKE_PREFIX + "user:" + userId;
    }

    /**
     * 某用户关注的实体（按时间排序）：
     * followee:{userId}:{entityType} -> zset(entityId, now)
     */
    public static String followeeZSet(Long userId, String entityType) {
        return FOLLOWEE_PREFIX + userId + ":" + entityType;
    }

    /**
     * 某实体的粉丝（按时间排序）：
     * follower:{entityType}:{entityId} -> zset(userId, now)
     */
    public static String followerZSet(String entityType, Long entityId) {
        return FOLLOWER_PREFIX + entityType + ":" + entityId;
    }

    public static String postLikeUsers(Long postId) {
        return PREFIX + "post:like:users:" + postId;
    }

    public static String postLikeCount(Long postId) {
        return PREFIX + "post:like:count:" + postId;
    }

    public static String postFavoriteUsers(Long postId) {
        return PREFIX + "post:favorite:users:" + postId;
    }

    public static String postFavoriteCount(Long postId) {
        return PREFIX + "post:favorite:count:" + postId;
    }

    public static String commentLikeUsers(Long commentId) {
        return PREFIX + "comment:like:users:" + commentId;
    }

    public static String commentLikeCount(Long commentId) {
        return PREFIX + "comment:like:count:" + commentId;
    }

    public static String userFollowings(Long followerId) {
        return PREFIX + "user:followings:" + followerId;
    }

    public static String userFollowers(Long followeeId) {
        return PREFIX + "user:followers:" + followeeId;
    }
}
