package com.campus.campus_life_backend.modules.auth.service;

import com.campus.campus_life_backend.modules.auth.entity.LoginAuditLog;
import com.campus.campus_life_backend.modules.auth.mapper.LoginAuditLogMapper;
import com.campus.campus_life_backend.modules.user.entity.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginAuditLogService {

    private final LoginAuditLogMapper loginAuditLogMapper;

    public LoginAuditLogService(LoginAuditLogMapper loginAuditLogMapper) {
        this.loginAuditLogMapper = loginAuditLogMapper;
    }

    public void recordSuccess(User user,
                              String account,
                              String loginType,
                              String ip,
                              String userAgent,
                              String sessionId) {
        LoginAuditLog log = new LoginAuditLog();
        log.setUserId(user == null ? null : user.getId());
        log.setAccount(trimToMax(account, 100));
        log.setRole(user == null ? null : user.getRole());
        log.setLoginType(normalizeLoginType(loginType));
        log.setSuccess(1);
        log.setIp(trimToMax(ip, 64));
        log.setUserAgent(trimToMax(userAgent, 512));
        log.setSessionId(trimToMax(sessionId, 64));
        loginAuditLogMapper.insert(log);
    }

    public void recordFailure(String account,
                              String loginType,
                              String failureReason,
                              String ip,
                              String userAgent) {
        LoginAuditLog log = new LoginAuditLog();
        log.setAccount(trimToMax(account, 100));
        log.setLoginType(normalizeLoginType(loginType));
        log.setSuccess(0);
        log.setFailureReason(trimToMax(failureReason, 255));
        log.setIp(trimToMax(ip, 64));
        log.setUserAgent(trimToMax(userAgent, 512));
        loginAuditLogMapper.insert(log);
    }

    public Map<String, Object> getMemberLoginLogs(Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;
        List<Map<String, Object>> list = loginAuditLogMapper.findMemberLoginLogs(offset, s);
        long total = loginAuditLogMapper.countMemberLoginLogs();
        return pageResult(list, p, s, total);
    }

    public Map<String, Object> getAdminLoginLogs(Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;
        List<Map<String, Object>> list = loginAuditLogMapper.findAdminLoginLogs(offset, s);
        long total = loginAuditLogMapper.countAdminLoginLogs();
        return pageResult(list, p, s, total);
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> list, int page, int size, long total) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", list == null ? List.of() : list);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        return result;
    }

    private String normalizeLoginType(String loginType) {
        return isBlank(loginType) ? "password" : trimToMax(loginType, 32);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
