package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    int insert(Report report);

    Report findById(@Param("id") Long id);

    Report findPendingByReporterAndTarget(
            @Param("reporterId") Long reporterId,
            @Param("targetType") Integer targetType,
            @Param("targetId") Long targetId
    );

    int processReport(
            @Param("id") Long id,
            @Param("status") Integer status,
            @Param("handlerId") Long handlerId,
            @Param("handleResult") String handleResult
    );

    List<Map<String, Object>> findAdminList(
            @Param("status") Integer status,
            @Param("targetType") Integer targetType,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    Long countAdminList(
            @Param("status") Integer status,
            @Param("targetType") Integer targetType,
            @Param("keyword") String keyword
    );

    long countByStatus(@Param("status") Integer status);
}
