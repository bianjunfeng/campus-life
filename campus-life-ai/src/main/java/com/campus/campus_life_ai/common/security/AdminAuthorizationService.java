package com.campus.campus_life_ai.common.security;

import org.springframework.stereotype.Service;

@Service
public class AdminAuthorizationService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final RoleAuthorizationService roleAuthorizationService;

    public AdminAuthorizationService(RoleAuthorizationService roleAuthorizationService) {
        this.roleAuthorizationService = roleAuthorizationService;
    }

    public void requireAdmin() {
        roleAuthorizationService.requireRole(ADMIN_ROLE, "仅管理员可访问 AI 管理接口");
    }
}
