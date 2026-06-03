package com.campus.campus_life_ai.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RoleAuthorizationServiceTest {

    private CurrentUserAccessor currentUserAccessor;
    private BackendUserProfileClient backendUserProfileClient;
    private RoleAuthorizationService roleAuthorizationService;

    @BeforeEach
    void setUp() {
        currentUserAccessor = mock(CurrentUserAccessor.class);
        backendUserProfileClient = mock(BackendUserProfileClient.class);
        roleAuthorizationService = new RoleAuthorizationService(currentUserAccessor, backendUserProfileClient);
    }

    @Test
    void shouldAllowExpectedRoleFromToken() {
        given(currentUserAccessor.requireUserId()).willReturn(1L);
        given(currentUserAccessor.getCurrentRole()).willReturn("MERCHANT");

        assertDoesNotThrow(() -> roleAuthorizationService.requireRole("MERCHANT", "仅商家可访问 AI 商家接口"));
        verifyNoInteractions(backendUserProfileClient);
    }

    @Test
    void shouldNormalizeNumericRoleFromToken() {
        given(currentUserAccessor.requireUserId()).willReturn(1L);
        given(currentUserAccessor.getCurrentRole()).willReturn("2");

        assertDoesNotThrow(() -> roleAuthorizationService.requireRole("ADMIN", "仅管理员可访问 AI 管理接口"));
        verifyNoInteractions(backendUserProfileClient);
    }

    @Test
    void shouldRejectUnexpectedRoleFromToken() {
        given(currentUserAccessor.requireUserId()).willReturn(2L);
        given(currentUserAccessor.getCurrentRole()).willReturn("STUDENT");

        assertThrows(ForbiddenException.class,
                () -> roleAuthorizationService.requireRole("ADMIN", "仅管理员可访问 AI 管理接口"));
        verifyNoInteractions(backendUserProfileClient);
    }

    @Test
    void shouldFallbackToBackendRoleForLegacyToken() {
        given(currentUserAccessor.requireUserId()).willReturn(1L);
        given(currentUserAccessor.getCurrentRole()).willReturn(null);
        given(currentUserAccessor.requireAuthorizationHeader()).willReturn("Bearer legacy-token");
        given(backendUserProfileClient.fetchCurrentUserRole("Bearer legacy-token")).willReturn("ADMIN");

        assertDoesNotThrow(() -> roleAuthorizationService.requireRole("ADMIN", "仅管理员可访问 AI 管理接口"));
        verify(backendUserProfileClient).fetchCurrentUserRole("Bearer legacy-token");
    }

    @Test
    void shouldRejectWhenBackendFallbackReturnsUnexpectedRole() {
        given(currentUserAccessor.requireUserId()).willReturn(3L);
        given(currentUserAccessor.getCurrentRole()).willReturn(null);
        given(currentUserAccessor.requireAuthorizationHeader()).willReturn("Bearer legacy-token");
        given(backendUserProfileClient.fetchCurrentUserRole("Bearer legacy-token")).willReturn("MERCHANT");

        assertThrows(ForbiddenException.class,
                () -> roleAuthorizationService.requireRole("ADMIN", "仅管理员可访问 AI 管理接口"));
    }
}
