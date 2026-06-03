package com.campus.campus_life_backend.modules.user.mapper;

import com.campus.campus_life_backend.modules.user.entity.StudentAuthRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生认证申请Mapper
 */
public interface StudentAuthRequestMapper {

    /**
     * 根据用户ID查询最新的认证申请
     */
    StudentAuthRequest findByUserId(@Param("userId") Long userId);

    /**
     * 插入认证申请
     */
    int insert(StudentAuthRequest request);

    /**
     * 更新认证申请
     */
    int update(StudentAuthRequest request);

    StudentAuthRequest findById(@Param("id") Long id);

    List<StudentAuthRequest> findAdminList(
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    long countAdminList(
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    long countByStatus(@Param("status") Integer status);

    int review(
            @Param("id") Long id,
            @Param("status") Integer status,
            @Param("reason") String reason,
            @Param("reviewerId") Long reviewerId
    );
}

