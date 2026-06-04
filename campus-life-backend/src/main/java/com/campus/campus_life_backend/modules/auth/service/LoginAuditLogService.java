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
        List<Map<String, Object>> list = normalizeLoginLogRows(loginAuditLogMapper.findMemberLoginLogs(offset, s), "memberId");
        long total = loginAuditLogMapper.countMemberLoginLogs();
        return pageResult(list, p, s, total);
    }

    public Map<String, Object> getAdminLoginLogs(Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;
        List<Map<String, Object>> list = normalizeLoginLogRows(loginAuditLogMapper.findAdminLoginLogs(offset, s), "adminId");
        long total = loginAuditLogMapper.countAdminLoginLogs();
        return pageResult(list, p, s, total);
    }

    private List<Map<String, Object>> normalizeLoginLogRows(List<Map<String, Object>> rows, String userIdAlias) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> normalizeLoginLogRow(row, userIdAlias))
                .toList();
    }

    private Map<String, Object> normalizeLoginLogRow(Map<String, Object> row, String userIdAlias) {
        Map<String, Object> normalized = new HashMap<>(row);
        Object userId = firstNonNull(normalized.get(userIdAlias), normalized.get("userId"));
        Object loginIp = firstNonNull(normalized.get("loginIp"), normalized.get("ip"));
        Object loginTime = firstNonNull(normalized.get("loginTime"), normalized.get("createTime"));

        putIfPresent(normalized, userIdAlias, userId);
        putIfPresent(normalized, "loginIp", loginIp);
        putIfPresent(normalized, "loginTime", loginTime);
        normalized.putIfAbsent("loginLocation", resolveIpLocation(loginIp));
        return normalized;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.putIfAbsent(key, value);
        }
    }

    private String resolveIpLocation(Object rawIp) {
        if (rawIp == null) {
            return null;
        }
        String ip = String.valueOf(rawIp).split(",", 2)[0].trim();
        if (ip.isEmpty()) {
            return null;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "本机";
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || isPrivate172Ip(ip)) {
            return "内网";
        }
        return "未知";
    }

    private boolean isPrivate172Ip(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
