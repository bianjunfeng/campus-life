package com.campus.campus_life_ai.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private RoleAuthorizationService roleAuthorizationService;

    @InjectMocks
    private AdminAuthorizationService adminAuthorizationService;

    @Test
    void shouldRequireAdminRole() {
        adminAuthorizationService.requireAdmin();
        verify(roleAuthorizationService).requireRole("ADMIN", "仅管理员可访问 AI 管理接口");
    }
}
