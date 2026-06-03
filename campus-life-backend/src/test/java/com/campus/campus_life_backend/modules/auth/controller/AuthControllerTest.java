package com.campus.campus_life_backend.modules.auth.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import com.campus.campus_life_backend.modules.auth.service.OAuthStateService;
import com.campus.campus_life_backend.modules.auth.service.VerificationService;
import com.campus.campus_life_backend.modules.auth.service.impl.QQOAuthService;
import com.campus.campus_life_backend.modules.auth.service.impl.WechatOAuthService;
import com.campus.campus_life_backend.modules.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private VerificationService verificationService;

    @Mock
    private OAuthStateService oauthStateService;

    @Mock
    private WechatOAuthService wechatOAuthService;

    @Mock
    private QQOAuthService qqOAuthService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldGenerateStateWhenBuildingWechatAuthorizationUrl() {
        String redirectUri = "https://example.com/oauth/wechat/callback";
        when(oauthStateService.createState("wechat", redirectUri)).thenReturn("state-123");
        when(authService.getWechatOAuthService()).thenReturn(wechatOAuthService);
        when(wechatOAuthService.getAuthorizationUrl(redirectUri, "state-123"))
                .thenReturn("https://wechat.example/auth?state=state-123");

        ApiResponse<Map<String, String>> response = authController.getWechatAuthUrl(redirectUri);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals("https://wechat.example/auth?state=state-123", response.getData().get("authUrl"));
        verify(oauthStateService).createState("wechat", redirectUri);
        verify(wechatOAuthService).getAuthorizationUrl(redirectUri, "state-123");
    }

    @Test
    void shouldHideWechatAuthorizationFailureDetails() {
        String redirectUri = "https://example.com/oauth/wechat/callback";
        when(oauthStateService.createState("wechat", redirectUri)).thenThrow(new IllegalStateException("redis://secret"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.getWechatAuthUrl(redirectUri));

        assertEquals(BusinessErrorCode.OAUTH_AUTHORIZE_URL_FAILED.getCode(), exception.getCode());
        assertEquals("获取微信授权URL失败", exception.getMessage());
    }

    @Test
    void shouldRejectWechatCallbackWhenStateIsInvalid() {
        String redirectUri = "https://example.com/oauth/wechat/callback";
        when(oauthStateService.validateAndConsumeState("wechat", "bad-state", redirectUri)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.wechatCallback("code-1", "bad-state", redirectUri, new MockHttpServletRequest()));

        assertEquals(BusinessErrorCode.OAUTH_STATE_INVALID.getCode(), exception.getCode());
        assertEquals("OAuth状态无效或已过期", exception.getMessage());
        verify(authService, never()).loginWithWechat("code-1", redirectUri);
    }

    @Test
    void shouldAllowQqCallbackWhenStateIsValid() {
        String redirectUri = "https://example.com/oauth/qq/callback";
        User user = new User();
        user.setId(1L);
        user.setUsername("qq-user");

        when(oauthStateService.validateAndConsumeState("qq", "state-ok", redirectUri)).thenReturn(true);
        when(authService.loginWithQQ("code-2", redirectUri)).thenReturn(user);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        when(authService.generateTokensForLogin(user, null, "qq", httpRequest))
                .thenReturn(Map.of("accessToken", "access-token", "refreshToken", "refresh-token", "sessionId", "session-1"));

        ApiResponse<Map<String, Object>> response = authController.qqCallback("code-2", "state-ok", redirectUri, httpRequest);

        assertEquals(200, response.getCode());
        assertEquals("access-token", response.getData().get("token"));
        assertEquals("access-token", response.getData().get("accessToken"));
        assertEquals("refresh-token", response.getData().get("refreshToken"));
        assertNotNull(response.getData().get("user"));
        verify(authService).loginWithQQ("code-2", redirectUri);
    }

    @Test
    void shouldHideUnexpectedRefreshFailureDetails() {
        when(authService.refreshTokens("refresh-token")).thenThrow(new IllegalStateException("jwt parser failed: secret"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.refreshToken(Map.of("refreshToken", "refresh-token")));

        assertEquals(BusinessErrorCode.INTERNAL_ERROR.getCode(), exception.getCode());
        assertEquals("刷新令牌失败，请稍后重试", exception.getMessage());
    }

    @Test
    void shouldReturn401ForRefreshTokenBusinessError() {
        when(authService.refreshTokens("refresh-token")).thenThrow(new BusinessException(BusinessErrorCode.REFRESH_TOKEN_EXPIRED));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.refreshToken(Map.of("refreshToken", "refresh-token")));

        assertEquals(BusinessErrorCode.REFRESH_TOKEN_EXPIRED.getCode(), exception.getCode());
        assertEquals("刷新令牌已失效", exception.getMessage());
    }

    @Test
    void shouldLogoutUsingCurrentSecurityContext() {
        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(currentUserAccessor.getCurrentAccessToken()).thenReturn("access-token");

        ApiResponse<String> response = authController.logout();

        assertEquals(200, response.getCode());
        assertEquals("登出成功", response.getData());
        verify(authService).logout(1L, "access-token");
    }

    @Test
    void shouldHideUnexpectedLogoutFailureDetails() {
        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(currentUserAccessor.getCurrentAccessToken()).thenReturn("access-token");
        org.mockito.Mockito.doThrow(new IllegalStateException("redis secret"))
                .when(authService)
                .logout(1L, "access-token");

        BusinessException exception = assertThrows(BusinessException.class, () -> authController.logout());

        assertEquals(BusinessErrorCode.INTERNAL_ERROR.getCode(), exception.getCode());
        assertEquals("登出失败，请稍后重试", exception.getMessage());
    }

    @Test
    void shouldLogoutAllDevicesForCurrentUser() {
        when(currentUserAccessor.requireUserId()).thenReturn(1L);

        ApiResponse<String> response = authController.logoutAll();

        assertEquals(200, response.getCode());
        assertEquals("全部设备已退出", response.getData());
        verify(authService).logoutAll(1L);
    }

    @Test
    void shouldLogoutOtherDevicesUsingCurrentSessionId() {
        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(currentUserAccessor.getCurrentAccessToken()).thenReturn("access-token");
        when(jwtUtil.getSessionIdFromToken("access-token")).thenReturn("session-current");

        ApiResponse<String> response = authController.logoutOthers();

        assertEquals(200, response.getCode());
        assertEquals("其他设备已退出", response.getData());
        verify(authService).logoutOthers(1L, "session-current");
    }

    @Test
    void shouldReturnCurrentUserSessions() {
        when(currentUserAccessor.requireUserId()).thenReturn(1L);
        when(authService.findUserSessions(1L)).thenReturn(java.util.List.of(Map.of("sessionId", "session-1")));

        ApiResponse<java.util.List<Map<String, Object>>> response = authController.sessions();

        assertEquals(200, response.getCode());
        assertEquals("session-1", response.getData().get(0).get("sessionId"));
        verify(authService).findUserSessions(1L);
    }

    @Test
    void shouldRevokeSpecifiedSessionForCurrentUser() {
        when(currentUserAccessor.requireUserId()).thenReturn(1L);

        ApiResponse<String> response = authController.revokeSession("session-1");

        assertEquals(200, response.getCode());
        assertEquals("会话已退出", response.getData());
        verify(authService).revokeUserSession(1L, "session-1");
    }
}
