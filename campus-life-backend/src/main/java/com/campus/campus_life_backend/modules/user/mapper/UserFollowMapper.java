package com.campus.campus_life_backend.modules.user.mapper;

import com.campus.campus_life_backend.modules.user.entity.UserFollow;
import org.apache.ibatis.annotations.Param;
import com.campus.campus_life_backend.modules.user.dto.UserFollowDTO;
import java.util.List;

public interface UserFollowMapper {

    int insertUserFollow(UserFollow userFollow);

    UserFollow findByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    List<UserFollow> findFollowersByUserId(@Param("userId") Long userId);

    List<UserFollow> findFollowingsByUserId(@Param("userId") Long userId);

    // 查询粉丝列表（带用户信息�?
    List<UserFollowDTO> findFollowersWithUserInfo(@Param("userId") Long userId);

    // 查询关注列表（带用户信息�?
    List<UserFollowDTO> findFollowingsWithUserInfo(@Param("userId") Long userId);

    // 查询互相关注列表（带用户信息�?
    List<UserFollowDTO> findMutualFollowsWithUserInfo(@Param("userId") Long userId);

    List<Long> findFollowedUserIdsByFollowerIdAndFolloweeIds(
            @Param("followerId") Long followerId,
            @Param("followeeIds") List<Long> followeeIds
    );

    int deleteUserFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int countFollowers(@Param("userId") Long userId);

    int countFollowings(@Param("userId") Long userId);
}


