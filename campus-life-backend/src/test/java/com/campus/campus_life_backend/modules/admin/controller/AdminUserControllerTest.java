package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.UserAdminOrchestrationService;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserAdminOrchestrationService userAdminService;

    @Mock
    private AdminAuthUtil adminAuthUtil;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminUserController controller;

    @Test
    void forceLogoutUserShouldRevokeTargetUserSessions() {
        given(currentUserAccessor.requireUserId()).willReturn(99L);

        ApiResponse<Map<String, Object>> response = controller.forceLogoutUser(7L);

        assertEquals(200, response.getCode());
        assertEquals(7L, response.getData().get("userId"));
        assertEquals(true, response.getData().get("forced"));
        verify(authService).forceLogoutUser(7L);
    }
}
