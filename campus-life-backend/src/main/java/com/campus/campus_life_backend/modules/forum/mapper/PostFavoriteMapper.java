package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.PostFavorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostFavoriteMapper {

    int insertPostFavorite(PostFavorite postFavorite);

    PostFavorite findByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    List<PostFavorite> findByUserId(@Param("userId") Long userId);

    List<Long> findPostIdsByUserIdWithPagination(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    List<Long> findFavoritedPostIdsByUserIdAndPostIds(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds
    );

    int deletePostFavorite(@Param("postId") Long postId, @Param("userId") Long userId);
}


