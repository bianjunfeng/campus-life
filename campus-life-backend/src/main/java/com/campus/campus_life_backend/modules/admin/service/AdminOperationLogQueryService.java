package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminOperationLogQueryService {

    private final AdminOperationLogMapper operationLogMapper;

    public AdminOperationLogQueryService(AdminOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public Map<String, Object> getOperationLogList(Integer page, Integer size, Long adminId, String operationType,
                                                   String targetType, String action, String startTime, String endTime) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : size;
        int offset = (safePage - 1) * safeSize;

        List<AdminOperationLog> logs = operationLogMapper.selectByConditions(
                adminId, operationType, targetType, action, startTime, endTime, offset, safeSize
        );
        Long total = operationLogMapper.countByConditions(
                adminId, operationType, targetType, action, startTime, endTime
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", logs == null ? List.of() : logs);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total == null ? 0 : total);
        return result;
    }
}
