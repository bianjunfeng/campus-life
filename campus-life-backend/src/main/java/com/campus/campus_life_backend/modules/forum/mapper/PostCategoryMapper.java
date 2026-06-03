package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.PostCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostCategoryMapper {

    int insertCategory(PostCategory category);

    PostCategory findById(@Param("id") Long id);

    PostCategory findByCode(@Param("code") String code);

    List<PostCategory> findAllActive();

    int updateCategory(PostCategory category);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}


