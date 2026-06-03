package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.dto.LoginRequest;
import com.campus.campus_life_backend.modules.auth.dto.RegisterRequest;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import com.campus.campus_life_backend.modules.auth.service.LoginAuditLogService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.auth.service.VerificationService;
import com.campus.campus_life_backend.modules.auth.service.impl.QQOAuthService;
import com.campus.campus_life_backend.modules.auth.service.impl.WechatOAuthService;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.search.event.UserSearchEventPublisher;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.StudentProfileMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private VerificationService verificationService;

    @Mock
    private StudentProfileMapper studentProfileMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private WechatOAuthService wechatOAuthService;

    @Mock
    private QQOAuthService qqOAuthService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private LoginAuditLogService loginAuditLogService;

    @Mock
    private AdminAuthUtil adminAuthUtil;

    @Mock
    private ObjectProvider<UserSearchService> userSearchServiceProvider;

    @Mock
    private ObjectProvider<UserSearchEventPublisher> userSearchEventPublisherProvider;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    public void testRegister() {
        // 准备测试数据
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPhone("13800138000");
        request.setPassword("123456");
        request.setCode("123456");
        request.setSchool("测试大学");
        request.setStudentNo("20250001");

        // 模拟方法调用
        when(verificationService.verifyCode("13800138000", "123456", "register")).thenReturn(true);
        when(userMapper.findByPhone(anyString())).thenReturn(null);
        when(userMapper.findByUsername(anyString())).thenReturn(null);
        when(studentProfileMapper.findBySchoolAndStudentNo(anyString(), anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        // 执行测试
        User user = authService.register(request);
        
        // 验证结果
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("13800138000", user.getPhone());
        assertEquals("encodedPassword", user.getPasswordHash());
        verify(userMapper).insertUser(user);
    }
    
    @Test
    public void testLoginWithCode() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setStatus(1);
        
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");
        
        // 模拟方法调用
        when(userMapper.findByPhone("13800138000")).thenReturn(user);
        when(verificationService.verifyCode("13800138000", "123456", "login")).thenReturn(true);
        
        // 执行测试
        User result = authService.login(request);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    public void testLoginWithPassword() {
        // 准备测试数据
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setPasswordHash("$2a$10$mockedBcryptHashForTestingOnly123456789012");
        user.setStatus(1);
        
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("123456");
        
        // 模拟方法调用
        when(userMapper.findByPhone("13800138000")).thenReturn(user);
        when(passwordEncoder.matches("123456", "$2a$10$mockedBcryptHashForTestingOnly123456789012")).thenReturn(true);
        
        // 执行测试
        User result = authService.login(request);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
    
    @Test
    public void testUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("123456");
        
        when(userMapper.findByPhone("13800138000")).thenReturn(null);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });
        
        assertEquals("用户不存在，请先注册", exception.getMessage());
    }

    @Test
    public void testLogoutShouldRevokeOnlyCurrentSession() {
        String accessToken = "access-token";
        long now = System.currentTimeMillis();

        when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken(accessToken)).thenReturn("session-1");
        when(jwtUtil.getExpirationDateFromToken(accessToken)).thenReturn(new Date(now + 60_000));

        authService.logout(1L, accessToken);

        verify(tokenService).revokeSession(1L, "session-1", AuthSessionService.STATUS_LOGGED_OUT);
        verify(tokenService).revokeTokenPair(1L, accessToken);
        verify(tokenService).blacklistAccessToken(eq(1L), eq(accessToken), anyLong());
        verify(tokenService, never()).revokeAllUserTokens(eq(1L), anyInt());
    }

    @Test
    public void testLogoutAllShouldRevokeAllUserSessionsAsLoggedOut() {
        authService.logoutAll(1L);

        verify(tokenService).revokeAllUserTokens(1L, AuthSessionService.STATUS_LOGGED_OUT);
    }

    @Test
    public void testLogoutOthersShouldKeepCurrentSession() {
        authService.logoutOthers(1L, "session-current");

        verify(tokenService).revokeOtherUserSessions(1L, "session-current", AuthSessionService.STATUS_LOGGED_OUT);
        verify(tokenService, never()).revokeAllUserTokens(eq(1L), anyInt());
    }

    @Test
    public void testRevokeUserSessionShouldRejectOtherUserSession() {
        when(authSessionService.sessionBelongsToUser("session-other", 1L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.revokeUserSession(1L, "session-other"));

        assertEquals(BusinessErrorCode.FORBIDDEN.getCode(), exception.getCode());
        verify(tokenService, never()).revokeSession(anyLong(), anyString(), anyInt());
    }

    @Test
    public void testForceLogoutUserShouldRevokeAllSessionsAsForceLogout() {
        authService.forceLogoutUser(7L);

        verify(tokenService).revokeAllUserTokens(7L, AuthSessionService.STATUS_FORCE_LOGOUT);
    }

    @Test
    public void testGenerateTokensForLoginShouldCreateSessionAndAuditSuccess() {
        long now = System.currentTimeMillis();
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setRole(0);

        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setPassword("password123");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("User-Agent", "JUnit Browser");
        httpRequest.setRemoteAddr("127.0.0.1");

        when(userMapper.findById(1L)).thenReturn(user);
        when(jwtUtil.generateAccessToken(eq(1L), eq(RoleCode.STUDENT), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(eq(1L), eq(RoleCode.STUDENT), anyString())).thenReturn("refresh-token");
        when(jwtUtil.getExpirationDateFromToken("access-token")).thenReturn(new Date(now + 60_000));
        when(adminAuthUtil.getClientIp(httpRequest)).thenReturn("127.0.0.1");

        Map<String, String> tokens = authService.generateTokensForLogin(user, request, httpRequest);

        assertEquals("access-token", tokens.get("accessToken"));
        assertEquals("refresh-token", tokens.get("refreshToken"));
        assertNotNull(tokens.get("sessionId"));
        verify(tokenService).storeTokens(eq(1L), eq(tokens.get("sessionId")), eq("access-token"), eq("refresh-token"), anyLong());
        verify(authSessionService).createOrUpdateSession(
                eq(user),
                eq(tokens.get("sessionId")),
                eq("access-token"),
                eq("refresh-token"),
                eq("web"),
                eq("127.0.0.1"),
                eq("JUnit Browser")
        );
        verify(loginAuditLogService).recordSuccess(user, "13800138000", "password", "127.0.0.1", "JUnit Browser", tokens.get("sessionId"));
    }

    @Test
    public void testRefreshTokensShouldRotateRefreshTokenAndInvalidateOldAccessToken() {
        long now = System.currentTimeMillis();
        String oldRefreshToken = "old-refresh-token";
        String oldAccessToken = "old-access-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        String sessionId = "session-1";

        User user = new User();
        user.setId(1L);
        user.setRole(0);

        stubValidRefreshToken(oldRefreshToken, 1L, "old-refresh-jti", sessionId);
        when(tokenService.tryAcquireRefreshLock("old-refresh-jti", "1:" + sessionId)).thenReturn(true);
        when(tokenService.consumeRefreshToken(1L, oldRefreshToken)).thenReturn(oldAccessToken);
        when(jwtUtil.getExpirationDateFromToken(oldRefreshToken)).thenReturn(new Date(now + 2_592_000_000L));
        when(jwtUtil.getExpirationDateFromToken(oldAccessToken)).thenReturn(new Date(now + 60_000L));
        when(userMapper.findById(1L)).thenReturn(user);
        when(jwtUtil.generateAccessToken(1L, RoleCode.STUDENT, sessionId)).thenReturn(newAccessToken);
        when(jwtUtil.generateRefreshToken(1L, RoleCode.STUDENT, sessionId)).thenReturn(newRefreshToken);
        when(jwtUtil.getExpirationDateFromToken(newAccessToken)).thenReturn(new Date(now + 1_800_000L));

        Map<String, String> tokens = authService.refreshTokens(oldRefreshToken);

        assertEquals(newAccessToken, tokens.get("accessToken"));
        assertEquals(newRefreshToken, tokens.get("refreshToken"));
        verify(tokenService).consumeRefreshToken(1L, oldRefreshToken);
        verify(tokenService).markRefreshTokenUsed(eq("old-refresh-jti"), eq(1L), eq(sessionId), anyLong());
        verify(tokenService).blacklistAccessTokenForRefresh(eq(1L), eq(oldAccessToken), anyLong());
        verify(tokenService).storeTokens(eq(1L), eq(sessionId), eq(newAccessToken), eq(newRefreshToken), anyLong());
        verify(authSessionService).updateTokenPair(sessionId, newAccessToken, newRefreshToken);
        verify(tokenService).releaseRefreshLock("old-refresh-jti");
    }

    @Test
    public void testRefreshTokenReuseShouldRevokeAllUserTokens() {
        String oldRefreshToken = "old-refresh-token";
        stubValidRefreshToken(oldRefreshToken, 1L, "old-refresh-jti", "session-1");
        when(tokenService.isRefreshTokenUsed("old-refresh-jti")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshTokens(oldRefreshToken));

        assertEquals(BusinessErrorCode.REFRESH_TOKEN_REUSED.getCode(), exception.getCode());
        verify(tokenService).revokeAllUserTokens(1L);
        verify(loginAuditLogService).recordFailure("1", "refresh", "REFRESH_TOKEN_REUSE", null, null);
        verify(tokenService, never()).tryAcquireRefreshLock(anyString(), anyString());
    }

    @Test
    public void testRefreshTokenMissingInRedisShouldBeTreatedAsReuse() {
        String oldRefreshToken = "old-refresh-token";
        stubValidRefreshToken(oldRefreshToken, 1L, "old-refresh-jti", "session-1");
        when(tokenService.tryAcquireRefreshLock("old-refresh-jti", "1:session-1")).thenReturn(true);
        when(tokenService.consumeRefreshToken(1L, oldRefreshToken)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshTokens(oldRefreshToken));

        assertEquals(BusinessErrorCode.REFRESH_TOKEN_REUSED.getCode(), exception.getCode());
        verify(tokenService).revokeAllUserTokens(1L);
        verify(loginAuditLogService).recordFailure("1", "refresh", "REFRESH_TOKEN_REUSE", null, null);
        verify(tokenService).releaseRefreshLock("old-refresh-jti");
    }

    @Test
    public void testRepeatedRefreshShouldOnlyAllowFirstCall() {
        long now = System.currentTimeMillis();
        String oldRefreshToken = "old-refresh-token";
        String oldAccessToken = "old-access-token";
        String sessionId = "session-1";
        User user = new User();
        user.setId(1L);
        user.setRole(0);

        stubValidRefreshToken(oldRefreshToken, 1L, "old-refresh-jti", sessionId);
        when(tokenService.isRefreshTokenUsed("old-refresh-jti")).thenReturn(false, false, true);
        when(tokenService.tryAcquireRefreshLock("old-refresh-jti", "1:" + sessionId)).thenReturn(true);
        when(tokenService.consumeRefreshToken(1L, oldRefreshToken)).thenReturn(oldAccessToken);
        when(jwtUtil.getExpirationDateFromToken(oldRefreshToken)).thenReturn(new Date(now + 2_592_000_000L));
        when(jwtUtil.getExpirationDateFromToken(oldAccessToken)).thenReturn(new Date(now + 60_000L));
        when(userMapper.findById(1L)).thenReturn(user);
        when(jwtUtil.generateAccessToken(1L, RoleCode.STUDENT, sessionId)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(1L, RoleCode.STUDENT, sessionId)).thenReturn("new-refresh-token");
        when(jwtUtil.getExpirationDateFromToken("new-access-token")).thenReturn(new Date(now + 1_800_000L));

        Map<String, String> first = authService.refreshTokens(oldRefreshToken);
        BusinessException second = assertThrows(BusinessException.class,
                () -> authService.refreshTokens(oldRefreshToken));

        assertEquals("new-access-token", first.get("accessToken"));
        assertEquals(BusinessErrorCode.REFRESH_TOKEN_REUSED.getCode(), second.getCode());
        verify(tokenService, times(1)).consumeRefreshToken(1L, oldRefreshToken);
        verify(tokenService).revokeAllUserTokens(1L);
    }

    @Test
    public void testRefreshShouldFailClosedWhenRedisUnavailable() {
        doThrow(new BusinessException(BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE))
                .when(tokenService)
                .requireTokenStoreAvailable();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshTokens("refresh-token"));

        assertEquals(BusinessErrorCode.REFRESH_TOKEN_EXPIRED.getCode(), exception.getCode());
        assertEquals("刷新令牌已失效，请重新登录", exception.getMessage());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    public void testRecordLoginFailureShouldWriteAuditLog() {
        LoginRequest request = new LoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("User-Agent", "JUnit Mobile");
        when(adminAuthUtil.getClientIp(httpRequest)).thenReturn("10.0.0.1");

        authService.recordLoginFailure(request, "验证码错误", httpRequest);

        verify(loginAuditLogService).recordFailure("13800138000", "code", "验证码错误", "10.0.0.1", "JUnit Mobile");
    }

    private void stubValidRefreshToken(String refreshToken, Long userId, String refreshJti, String sessionId) {
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(jwtUtil.getJwtIdFromToken(refreshToken)).thenReturn(refreshJti);
        when(jwtUtil.getSessionIdFromToken(refreshToken)).thenReturn(sessionId);
    }
}
