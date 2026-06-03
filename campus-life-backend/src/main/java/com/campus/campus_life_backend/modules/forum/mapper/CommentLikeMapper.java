package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.CommentLike;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentLikeMapper {

    int insertCommentLike(CommentLike commentLike);

    CommentLike findByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    List<Long> findLikedCommentIdsByUserIdAndCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds
    );

    int deleteCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
