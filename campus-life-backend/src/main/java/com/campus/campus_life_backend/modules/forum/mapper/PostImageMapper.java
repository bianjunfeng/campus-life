package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.PostImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostImageMapper {

    /**
     * 根据帖子ID查询所有图�?
     */
    List<PostImage> findByPostId(@Param("postId") Long postId);

    /**
     * 批量查询帖子图片，按帖子和图片排序返回
     */
    List<PostImage> findByPostIds(@Param("postIds") List<Long> postIds);

    /**
     * 插入帖子图片
     */
    int insertPostImage(PostImage postImage);

    /**
     * 删除帖子图片
     */
    int deleteByPostId(@Param("postId") Long postId);
}



