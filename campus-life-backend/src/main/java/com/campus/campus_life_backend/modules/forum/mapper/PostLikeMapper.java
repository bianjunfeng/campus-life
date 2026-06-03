package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.PostLike;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostLikeMapper {

    int insertPostLike(PostLike postLike);

    PostLike findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    List<Long> findLikedPostIdsByUserIdAndPostIds(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds
    );

    int deletePostLike(@Param("postId") Long postId, @Param("userId") Long userId);

    int countByPostId(@Param("postId") Long postId);
}


