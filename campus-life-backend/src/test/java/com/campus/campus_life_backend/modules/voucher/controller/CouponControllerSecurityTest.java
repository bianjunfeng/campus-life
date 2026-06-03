package com.campus.campus_life_backend.modules.voucher.controller;

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
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherSeckillService;
import com.campus.campus_life_backend.modules.voucher.service.WelfareQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        WebMvcSecurityConfig.class,
        AuthorizationService.class,
        CurrentUserAccessor.class
})
class CouponControllerSecurityTest {

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
    private WelfareQueryService welfareQueryService;

    @MockitoBean
    private VoucherSeckillService voucherSeckillService;

    @Test
    void shouldRejectGrabCouponWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/coupons/9/grab"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.LOGIN_REQUIRED.getCode()));

        verifyNoInteractions(voucherSeckillService);
    }

    @Test
    void shouldRejectGrabCouponWhenPermissionMissing() throws Exception {
        stubAuthenticatedUser("limited-token", Set.of());

        mockMvc.perform(post("/api/coupons/9/grab")
                        .header("Authorization", "Bearer limited-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.message").value("缺少任一权限: voucher:order:self"));

        verifyNoInteractions(voucherSeckillService);
    }

    @Test
    void shouldAllowGrabCouponWhenPermissionGranted() throws Exception {
        stubAuthenticatedUser("valid-token", Set.of("voucher:order:self"));
        when(voucherSeckillService.submitSeckill(9L, 1L))
                .thenReturn(Map.of("success", true, "orderNo", "ORD-9001"));

        mockMvc.perform(post("/api/coupons/9/grab")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("ORD-9001"));

        verify(voucherSeckillService).submitSeckill(9L, 1L);
    }

    @Test
    void shouldAllowQueryGrabResultWhenPermissionGranted() throws Exception {
        stubAuthenticatedUser("valid-token", Set.of("voucher:order:self"));
        when(voucherSeckillService.querySeckillResult("ORD-9001", 1L))
                .thenReturn(Map.of("status", "PENDING"));

        mockMvc.perform(get("/api/coupons/grab/result")
                        .param("orderNo", "ORD-9001")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(voucherSeckillService).querySeckillResult("ORD-9001", 1L);
    }

    private void stubAuthenticatedUser(String token, Set<String> permissions) {
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getTokenType(token)).thenReturn("access");
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(tokenService.isAccessTokenValid(1L, token)).thenReturn(true);
        when(loginPrincipalFactory.create(1L))
                .thenReturn(new LoginPrincipal(1L, RoleCode.STUDENT, permissions));
    }
}
