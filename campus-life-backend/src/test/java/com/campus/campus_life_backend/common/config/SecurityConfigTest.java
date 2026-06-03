package com.campus.campus_life_backend.common.config;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.ownership.ResourceOwnershipService;
import com.campus.campus_life_backend.common.security.service.LoginPrincipalFactory;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.security.service.AuthorizationService;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.message.controller.MessageController;
import com.campus.campus_life_backend.modules.message.dto.ConversationDTO;
import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.message.service.MessageService;
import com.campus.campus_life_backend.modules.search.controller.SearchController;
import com.campus.campus_life_backend.modules.search.service.PostSearchService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsService;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SearchController.class, MessageController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        WebMvcSecurityConfig.class,
        AuthorizationService.class,
        CurrentUserAccessor.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private LoginPrincipalFactory loginPrincipalFactory;

    @MockitoBean
    private AuthSessionService authSessionService;

    @MockitoBean
    private ResourceOwnershipService resourceOwnershipService;

    @MockitoBean
    private PostSearchService postSearchService;

    @MockitoBean
    private UserSearchService userSearchService;

    @MockitoBean
    private SearchOpsService searchOpsService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private MessageNotificationService messageNotificationService;

    @Test
    void shouldAllowPublicEndpointWithoutAuthentication() throws Exception {
        when(searchOpsService.getHealthStatus()).thenReturn(Map.of("status", "UP", "service", "search"));

        mockMvc.perform(get("/api/search/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));

        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectProtectedEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/message/conversations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.LOGIN_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void shouldAllowProtectedEndpointWithValidToken() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getTokenType("valid-token")).thenReturn("access");
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(tokenService.isAccessTokenValid(1L, "valid-token")).thenReturn(true);
        when(loginPrincipalFactory.create(1L))
                .thenReturn(new LoginPrincipal(1L, RoleCode.STUDENT, Set.of("message:use")));
        when(messageService.getConversationsByUserId(1L)).thenReturn(List.<ConversationDTO>of());

        mockMvc.perform(get("/api/message/conversations")
                        .header("Authorization", "Bearer valid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}

