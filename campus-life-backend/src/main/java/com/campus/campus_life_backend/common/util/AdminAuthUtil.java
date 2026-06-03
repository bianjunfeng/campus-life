package com.campus.campus_life_backend.common.util;

import org.springframework.stereotype.Component;

/**
 * 管理员请求辅助工具类
 * 仅保留与鉴权无关的通用能力，例如客户端 IP 提取。
 */
@Component
public class AdminAuthUtil {

    /**
     * 获取客户端IP地址
     */
    public String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}


