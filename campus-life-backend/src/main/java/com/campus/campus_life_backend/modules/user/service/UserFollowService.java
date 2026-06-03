package com.campus.campus_life_backend.modules.user.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.redis.RedisSocialKeys;
import com.campus.campus_life_backend.modules.user.entity.UserFollow;
import com.campus.campus_life_backend.modules.user.dto.UserFollowDTO;
import com.campus.campus_life_backend.modules.user.mapper.UserFollowMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEvent;
import com.campus.campus_life_backend.modules.message.event.SystemNotificationEventPublisher;
import com.campus.campus_life_backend.modules.search.event.UserSearchEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserFollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final SystemNotificationEventPublisher systemNotificationEventPublisher;
    private final UserSearchEventPublisher userSearchEventPublisher;
    private static final String USER_ENTITY_TYPE = "user";

    public UserFollowService(
            UserFollowMapper userFollowMapper,
            UserMapper userMapper,
            ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider,
            ObjectProvider<SystemNotificationEventPublisher> systemNotificationEventPublisherProvider,
            ObjectProvider<UserSearchEventPublisher> userSearchEventPublisherProvider) {
        this.userFollowMapper = userFollowMapper;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.systemNotificationEventPublisher = systemNotificationEventPublisherProvider.getIfAvailable();
        this.userSearchEventPublisher = userSearchEventPublisherProvider == null ? null : userSearchEventPublisherProvider.getIfAvailable();
    }

    @Transactional
    public boolean followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException(BusinessErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        Boolean redisFollow = checkFollowFromRedis(followerId, followeeId);
        if (Boolean.TRUE.equals(redisFollow)) {
            return true;
        }

        UserFollow existing = userFollowMapper.findByFollowerIdAndFolloweeId(followerId, followeeId);
        if (existing != null) {
            syncFollowToRedis(followerId, followeeId, true);
            return true; // 已经关注
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(followerId);
        userFollow.setFolloweeId(followeeId);
        userFollowMapper.insertUserFollow(userFollow);
        syncFollowToRedis(followerId, followeeId, true);
        publishFollowNotification(followerId, followeeId);
        publishUserSearchUpsert(followeeId, "follow_user");
        return true;
    }

    @Transactional
    public boolean unfollowUser(Long followerId, Long followeeId) {
        userFollowMapper.deleteUserFollow(followerId, followeeId);
        syncFollowToRedis(followerId, followeeId, false);
        publishUserSearchUpsert(followeeId, "unfollow_user");
        return false;
    }

    public List<UserFollow> getFollowers(Long userId) {
        return userFollowMapper.findFollowersByUserId(userId);
    }

    public List<UserFollow> getFollowings(Long userId) {
        return userFollowMapper.findFollowingsByUserId(userId);
    }

    /**
     * 获取粉丝列表（带用户信息）
     */
    public List<UserFollowDTO> getFollowersWithUserInfo(Long userId, Long currentUserId) {
        List<UserFollowDTO> followers = readFollowersFromRedis(userId);
        if (followers.isEmpty()) {
            followers = userFollowMapper.findFollowersWithUserInfo(userId);
            warmFollowListToRedis(followers, false);
        }
        if (currentUserId != null) {
            Set<Long> followedUserIds = findFollowedAuthorIds(currentUserId, followers.stream()
                    .map(UserFollowDTO::getUserId)
                    .filter(id -> id != null)
                    .toList());
            for (UserFollowDTO dto : followers) {
                dto.setIsFollowing(followedUserIds.contains(dto.getUserId()));
            }
        }
        return followers;
    }

    /**
     * 获取关注列表（带用户信息）
     */
    public List<UserFollowDTO> getFollowingsWithUserInfo(Long userId, Long currentUserId) {
        List<UserFollowDTO> followings = readFollowingsFromRedis(userId);
        if (followings.isEmpty()) {
            followings = userFollowMapper.findFollowingsWithUserInfo(userId);
            warmFollowListToRedis(followings, true);
        }
        if (currentUserId != null) {
            Set<Long> mutualUserIds = findFollowedAuthorIds(userId, followings.stream()
                    .map(UserFollowDTO::getUserId)
                    .filter(id -> id != null)
                    .toList());
            for (UserFollowDTO dto : followings) {
                dto.setIsMutual(mutualUserIds.contains(dto.getUserId()));
            }
        }
        return followings;
    }

    /**
     * 获取互相关注列表（带用户信息）
     */
    public List<UserFollowDTO> getMutualFollowsWithUserInfo(Long userId) {
        List<UserFollowDTO> list = readMutualFromRedis(userId);
        if (!list.isEmpty()) {
            return list;
        }
        return userFollowMapper.findMutualFollowsWithUserInfo(userId);
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        Boolean redisFollow = checkFollowFromRedis(followerId, followeeId);
        if (redisFollow != null) {
            return redisFollow;
        }

        UserFollow follow = userFollowMapper.findByFollowerIdAndFolloweeId(followerId, followeeId);
        boolean result = follow != null;
        if (result) {
            syncFollowToRedis(followerId, followeeId, true);
        }
        return result;
    }

    public Set<Long> findFollowedAuthorIds(Long followerId, List<Long> followeeIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (followerId == null || followeeIds == null || followeeIds.isEmpty()) {
            return result;
        }

        List<Long> matchedIds = userFollowMapper.findFollowedUserIdsByFollowerIdAndFolloweeIds(followerId, followeeIds);
        if (matchedIds != null) {
            result.addAll(matchedIds);
            return result;
        }

        for (Long followeeId : followeeIds) {
            if (isFollowing(followerId, followeeId)) {
                result.add(followeeId);
            }
        }
        return result;
    }

    private Boolean checkFollowFromRedis(Long followerId, Long followeeId) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            Boolean member = setOps.isMember(RedisSocialKeys.userFollowings(followerId), followeeId.toString());
            if (Boolean.TRUE.equals(member)) {
                return true;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void syncFollowToRedis(Long followerId, Long followeeId, boolean following) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String followingsKey = RedisSocialKeys.userFollowings(followerId);
            String followersKey = RedisSocialKeys.userFollowers(followeeId);
            String followeeZSetKey = RedisSocialKeys.followeeZSet(followerId, USER_ENTITY_TYPE);
            String followerZSetKey = RedisSocialKeys.followerZSet(USER_ENTITY_TYPE, followeeId);
            String followeeValue = followeeId.toString();
            String followerValue = followerId.toString();
            double now = System.currentTimeMillis();

            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings({"unchecked", "NullableProblems"})
                public Object execute(org.springframework.data.redis.core.RedisOperations operations) {
                    operations.multi();
                    if (following) {
                        operations.opsForSet().add(followingsKey, followeeValue);
                        operations.opsForSet().add(followersKey, followerValue);
                        operations.opsForZSet().add(followeeZSetKey, followeeValue, now);
                        operations.opsForZSet().add(followerZSetKey, followerValue, now);
                    } else {
                        operations.opsForSet().remove(followingsKey, followeeValue);
                        operations.opsForSet().remove(followersKey, followerValue);
                        operations.opsForZSet().remove(followeeZSetKey, followeeValue);
                        operations.opsForZSet().remove(followerZSetKey, followerValue);
                    }
                    return operations.exec();
                }
            });
        } catch (Exception e) {
            // Redis 异常不影响主流程
        }
    }

    private void publishFollowNotification(Long followerId, Long followeeId) {
        if (systemNotificationEventPublisher == null
                || followerId == null
                || followeeId == null
                || followerId.equals(followeeId)) {
            return;
        }
        SystemNotificationEvent event = new SystemNotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType("FOLLOW");
        event.setTargetUserId(followeeId);
        event.setActorUserId(followerId);
        event.setMessage("关注了你");
        event.setCreateEpochMillis(System.currentTimeMillis());
        systemNotificationEventPublisher.publish(event);
    }

    private void publishUserSearchUpsert(Long userId, String reason) {
        if (userSearchEventPublisher == null || userId == null) {
            return;
        }
        try {
            userSearchEventPublisher.publishUpsert(userId, reason);
        } catch (Exception ignored) {
        }
    }

    private List<UserFollowDTO> readFollowersFromRedis(Long userId) {
        if (redisTemplate == null) {
            return Collections.emptyList();
        }
        try {
            String zsetKey = RedisSocialKeys.followerZSet(USER_ENTITY_TYPE, userId);
            Set<String> followerIds = redisTemplate.opsForZSet().reverseRange(zsetKey, 0, -1);
            if (followerIds == null || followerIds.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, User> usersById = loadUsersByIds(followerIds);
            List<UserFollowDTO> result = new ArrayList<>();
            for (String idStr : followerIds) {
                Long uid = Long.valueOf(idStr);
                User user = usersById.get(uid);
                if (user == null) continue;
                UserFollowDTO dto = new UserFollowDTO();
                dto.setUserId(uid);
                dto.setFollowerId(uid);
                dto.setFolloweeId(userId);
                dto.setName(user.getUsername());
                dto.setAvatar(user.getAvatarUrl());
                dto.setBio(user.getBio());
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<UserFollowDTO> readFollowingsFromRedis(Long userId) {
        if (redisTemplate == null) {
            return Collections.emptyList();
        }
        try {
            String zsetKey = RedisSocialKeys.followeeZSet(userId, USER_ENTITY_TYPE);
            Set<String> followeeIds = redisTemplate.opsForZSet().reverseRange(zsetKey, 0, -1);
            if (followeeIds == null || followeeIds.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, User> usersById = loadUsersByIds(followeeIds);
            List<UserFollowDTO> result = new ArrayList<>();
            for (String idStr : followeeIds) {
                Long uid = Long.valueOf(idStr);
                User user = usersById.get(uid);
                if (user == null) continue;
                UserFollowDTO dto = new UserFollowDTO();
                dto.setUserId(uid);
                dto.setFollowerId(userId);
                dto.setFolloweeId(uid);
                dto.setName(user.getUsername());
                dto.setAvatar(user.getAvatarUrl());
                dto.setBio(user.getBio());
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<UserFollowDTO> readMutualFromRedis(Long userId) {
        if (redisTemplate == null) {
            return Collections.emptyList();
        }
        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            String followingsKey = RedisSocialKeys.userFollowings(userId);
            String followersKey = RedisSocialKeys.userFollowers(userId);
            Set<String> mutualIds = setOps.intersect(followingsKey, followersKey);
            if (mutualIds == null || mutualIds.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, User> usersById = loadUsersByIds(mutualIds);
            List<UserFollowDTO> result = new ArrayList<>();
            for (String idStr : new LinkedHashSet<>(mutualIds)) {
                Long uid = Long.valueOf(idStr);
                User user = usersById.get(uid);
                if (user == null) continue;
                UserFollowDTO dto = new UserFollowDTO();
                dto.setUserId(uid);
                dto.setFollowerId(userId);
                dto.setFolloweeId(uid);
                dto.setName(user.getUsername());
                dto.setAvatar(user.getAvatarUrl());
                dto.setBio(user.getBio());
                dto.setIsMutual(true);
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void warmFollowListToRedis(List<UserFollowDTO> list, boolean followingDirection) {
        if (redisTemplate == null || list == null || list.isEmpty()) {
            return;
        }
        for (UserFollowDTO dto : list) {
            Long followerId = followingDirection ? dto.getFollowerId() : dto.getUserId();
            Long followeeId = followingDirection ? dto.getUserId() : dto.getFolloweeId();
            if (followerId == null) {
                followerId = dto.getFollowerId();
            }
            if (followeeId == null) {
                followeeId = dto.getFolloweeId();
            }
            if (followerId == null || followeeId == null) {
                continue;
            }
            syncFollowToRedis(followerId, followeeId, true);
        }
    }

    private Map<Long, User> loadUsersByIds(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> numericIds = userIds.stream()
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toList());
        return loadUsersByIds(numericIds);
    }

    private Map<Long, User> loadUsersByIds(List<Long> userIds) {
        Map<Long, User> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        List<User> users = userMapper.findByIds(userIds);
        if (users != null) {
            for (User user : users) {
                result.put(user.getId(), user);
            }
            return result;
        }

        for (Long userId : userIds) {
            User user = userMapper.findById(userId);
            if (user != null) {
                result.put(userId, user);
            }
        }
        return result;
    }
}
