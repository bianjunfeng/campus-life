package com.campus.campus_life_backend.modules.auth.mapper;

import com.campus.campus_life_backend.modules.auth.entity.LoginAuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface LoginAuditLogMapper {

    int insert(LoginAuditLog log);

    List<Map<String, Object>> findMemberLoginLogs(@Param("offset") Integer offset, @Param("limit") Integer limit);

    long countMemberLoginLogs();

    List<Map<String, Object>> findAdminLoginLogs(@Param("offset") Integer offset, @Param("limit") Integer limit);

    long countAdminLoginLogs();
}
