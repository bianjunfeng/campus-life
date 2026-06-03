package com.campus.campus_life_ai.common.config;

import com.campus.campus_life_ai.common.security.RoleAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MerchantAiAuthorizationInterceptor implements HandlerInterceptor {

    private final RoleAuthorizationService roleAuthorizationService;

    public MerchantAiAuthorizationInterceptor(RoleAuthorizationService roleAuthorizationService) {
        this.roleAuthorizationService = roleAuthorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        roleAuthorizationService.requireRole("MERCHANT", "仅商家可访问 AI 商家接口");
        return true;
    }
}
