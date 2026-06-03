package com.campus.campus_life_backend.modules.admin.controller;

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
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.admin.service.SensitiveWordAdminService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSensitiveWordController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        WebMvcSecurityConfig.class,
        AuthorizationService.class,
        CurrentUserAccessor.class
})
class AdminSensitiveWordControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private LoginPrincipalFactory loginPrincipalFactory;

    @MockitoBean
    private ResourceOwnershipService resourceOwnershipService;

    @MockitoBean
    private SensitiveWordAdminService sensitiveWordAdminService;

    @MockitoBean
    private AdminAuthUtil adminAuthUtil;

    @Test
    void shouldRejectStudentRoleForSensitiveWordManagement() throws Exception {
        stubAuthenticated("student-token", RoleCode.STUDENT, Set.of());

        mockMvc.perform(get("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.FORBIDDEN.getCode()));

        verifyNoInteractions(sensitiveWordAdminService);
    }

    @Test
    void shouldAllowAdminRoleToListSensitiveWords() throws Exception {
        stubAuthenticated("admin-token", RoleCode.ADMIN, Set.of());
        when(sensitiveWordAdminService.listWords(null, 1, 50)).thenReturn(Map.of(
                "list", List.of("赌博"),
                "total", 1,
                "page", 1,
                "size", 50,
                "wordCount", 1
        ));

        mockMvc.perform(get("/api/admin/sensitive-words")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0]").value("赌博"));
    }

    private void stubAuthenticated(String token, RoleCode roleCode, Set<String> permissions) {
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getTokenType(token)).thenReturn("access");
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(tokenService.isAccessTokenValid(1L, token)).thenReturn(true);
        when(loginPrincipalFactory.create(1L)).thenReturn(new LoginPrincipal(1L, roleCode, permissions));
    }
}
