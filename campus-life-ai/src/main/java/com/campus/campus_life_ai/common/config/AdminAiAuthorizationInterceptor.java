package com.campus.campus_life_ai.common.config;

import com.campus.campus_life_ai.common.security.AdminAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAiAuthorizationInterceptor implements HandlerInterceptor {

    private final AdminAuthorizationService adminAuthorizationService;

    public AdminAiAuthorizationInterceptor(AdminAuthorizationService adminAuthorizationService) {
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        adminAuthorizationService.requireAdmin();
        return true;
    }
}
