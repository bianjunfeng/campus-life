package com.campus.campus_life_backend.modules.admin.mapper;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminOperationLogMapper {
    
    /**
     * 插入操作日志
     */
    void insert(AdminOperationLog log);
    
    /**
     * 根据ID查询
     */
    AdminOperationLog findById(@Param("id") Long id);
    
    /**
     * 查询操作日志列表
     */
    List<AdminOperationLog> findList(
        @Param("adminId") Long adminId,
        @Param("operationType") String operationType,
        @Param("targetType") String targetType,
        @Param("action") String action,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 统计操作日志数量
     */
    Long count(
        @Param("adminId") Long adminId,
        @Param("operationType") String operationType,
        @Param("targetType") String targetType,
        @Param("action") String action,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );

    List<AdminOperationLog> selectByConditions(
        @Param("adminId") Long adminId,
        @Param("operationType") String operationType,
        @Param("targetType") String targetType,
        @Param("action") String action,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    Long countByConditions(
        @Param("adminId") Long adminId,
        @Param("operationType") String operationType,
        @Param("targetType") String targetType,
        @Param("action") String action,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );

    Long countDistinctIpBetween(
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );
}


