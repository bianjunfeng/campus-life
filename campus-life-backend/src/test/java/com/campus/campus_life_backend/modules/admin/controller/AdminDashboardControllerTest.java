package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.admin.service.AdminApiCatalogService;
import com.campus.campus_life_backend.modules.admin.service.AdminDashboardQueryService;
import com.campus.campus_life_backend.modules.admin.service.AdminOperationLogQueryService;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.LoginAuditLogService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.search.service.PostSearchService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsService;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
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
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardQueryService dashboardQueryService;

    @Mock
    private AdminOperationLogQueryService operationLogQueryService;

    @Mock
    private AdminApiCatalogService adminApiCatalogService;

    @Mock
    private PostSearchService postSearchService;

    @Mock
    private UserSearchService userSearchService;

    @Mock
    private SearchOpsService searchOpsService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private LoginAuditLogService loginAuditLogService;

    @InjectMocks
    private AdminDashboardController controller;

    @Test
    void getOnlineUsersShouldReturnRealSessionPage() {
        given(currentUserAccessor.requireUserId()).willReturn(99L);
        Map<String, Object> page = Map.of(
                "list", java.util.List.of(Map.of("userId", 7L, "sessionId", "session-1")),
                "page", 1,
                "size", 20,
                "total", 1L,
                "totalOnline", 1L
        );
        given(authSessionService.getOnlineUsers(1, 20)).willReturn(page);

        ApiResponse<Map<String, Object>> response = controller.getOnlineUsers(1, 20);

        assertEquals(200, response.getCode());
        assertEquals(page, response.getData());
        verify(authSessionService).getOnlineUsers(1, 20);
    }

    @Test
    void getMemberLoginLogListShouldReturnRealAuditPage() {
        given(currentUserAccessor.requireUserId()).willReturn(99L);
        Map<String, Object> page = Map.of("list", java.util.List.of(Map.of("role", 0)), "page", 1, "size", 20, "total", 1L);
        given(loginAuditLogService.getMemberLoginLogs(1, 20)).willReturn(page);

        ApiResponse<Map<String, Object>> response = controller.getMemberLoginLogList(1, 20);

        assertEquals(200, response.getCode());
        assertEquals(page, response.getData());
        verify(loginAuditLogService).getMemberLoginLogs(1, 20);
    }

    @Test
    void getAdminLoginLogListShouldReturnRealAuditPage() {
        given(currentUserAccessor.requireUserId()).willReturn(99L);
        Map<String, Object> page = Map.of("list", java.util.List.of(Map.of("role", 2)), "page", 1, "size", 20, "total", 1L);
        given(loginAuditLogService.getAdminLoginLogs(1, 20)).willReturn(page);

        ApiResponse<Map<String, Object>> response = controller.getAdminLoginLogList(1, 20);

        assertEquals(200, response.getCode());
        assertEquals(page, response.getData());
        verify(loginAuditLogService).getAdminLoginLogs(1, 20);
    }

    @Test
    void forceLogoutUserShouldRevokeAllUserTokensAndReturnForcedTrue() {
        given(currentUserAccessor.requireUserId()).willReturn(99L);

        ApiResponse<Map<String, Object>> response = controller.forceLogoutUser(7L);

        verify(tokenService).revokeAllUserTokens(7L);
        assertEquals(200, response.getCode());
        assertEquals(7L, response.getData().get("userId"));
        assertEquals(true, response.getData().get("forced"));
    }
}
