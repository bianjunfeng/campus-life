package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentMapper {

    int insertComment(Comment comment);

    Comment findById(@Param("id") Long id);

    List<Comment> findByPostId(@Param("postId") Long postId);
    long countByPostId(@Param("postId") Long postId);

    List<Comment> findByUserId(@Param("userId") Long userId);

    List<Comment> findByParentId(@Param("parentId") Long parentId);

    // 管理后台：查询全部评论（包含已删除/屏蔽）
    List<Comment> findAllForAdmin();

    int updateComment(Comment comment);

    int deleteComment(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id);

    int decrementLikeCount(@Param("id") Long id);

    long countAllForAdmin();

    long countByStatusForAdmin(@Param("status") Integer status);
}
