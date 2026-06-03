package com.campus.campus_life_backend.modules.auth.controller;

import com.campus.campus_life_backend.common.config.JwtAuthenticationFilter;
import com.campus.campus_life_backend.common.config.SecurityConfig;
import com.campus.campus_life_backend.common.config.WebMvcSecurityConfig;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.ownership.ResourceOwnershipService;
import com.campus.campus_life_backend.common.security.service.AuthorizationService;
import com.campus.campus_life_backend.common.security.service.LoginPrincipalFactory;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import com.campus.campus_life_backend.modules.auth.service.OAuthStateService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.auth.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        WebMvcSecurityConfig.class,
        AuthorizationService.class,
        CurrentUserAccessor.class
})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private VerificationService verificationService;

    @MockitoBean
    private OAuthStateService oauthStateService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private AuthSessionService authSessionService;

    @MockitoBean
    private LoginPrincipalFactory loginPrincipalFactory;

    @MockitoBean
    private ResourceOwnershipService resourceOwnershipService;

    @Test
    void shouldRejectLogoutWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.LOGIN_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));

        verifyNoInteractions(authService);
    }

    @Test
    void shouldLogoutWithAuthenticatedToken() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getTokenType("valid-token")).thenReturn("access");
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(tokenService.isAccessTokenValid(1L, "valid-token")).thenReturn(true);
        when(loginPrincipalFactory.create(1L))
                .thenReturn(new LoginPrincipal(1L, RoleCode.STUDENT, Set.of()));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("登出成功"));

        verify(authService).logout(1L, "valid-token");
    }
}
