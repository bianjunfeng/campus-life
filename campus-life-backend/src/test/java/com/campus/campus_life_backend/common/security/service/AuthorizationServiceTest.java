package com.campus.campus_life_backend.common.security.service;

import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.exception.OwnershipDeniedException;
import com.campus.campus_life_backend.common.security.exception.PermissionDeniedException;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.ownership.ResourceOwnershipService;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private final CurrentUserAccessor currentUserAccessor = mock(CurrentUserAccessor.class);
    private final ResourceOwnershipService resourceOwnershipService = mock(ResourceOwnershipService.class);
    private final AuthorizationService authorizationService =
            new AuthorizationService(currentUserAccessor, resourceOwnershipService);

    @Test
    void shouldAllowStudentRoleWithOwnedPermission() throws Exception {
        LoginPrincipal principal = new LoginPrincipal(
                1L,
                RoleCode.STUDENT,
                Set.of("post:update:self", "message:use")
        );
        when(currentUserAccessor.requirePrincipal()).thenReturn(principal);

        RequireRole requireRole = annotationOf("studentOnly", RequireRole.class);
        RequirePermission requirePermission = annotationOf("studentOnly", RequirePermission.class);

        assertDoesNotThrow(() -> authorizationService.authorize(requireRole, requirePermission, null, null));
    }

    @Test
    void shouldRejectMissingRole() throws Exception {
        LoginPrincipal principal = new LoginPrincipal(2L, RoleCode.STUDENT, Set.of("message:use"));
        when(currentUserAccessor.requirePrincipal()).thenReturn(principal);

        RequireRole requireRole = annotationOf("adminOnly", RequireRole.class);

        assertThrows(PermissionDeniedException.class,
                () -> authorizationService.authorize(requireRole, null, null, null));
    }

    @Test
    void shouldAllowOwnerEvenWithoutFallbackPermission() throws Exception {
        LoginPrincipal principal = new LoginPrincipal(9L, RoleCode.STUDENT, Set.of());
        when(currentUserAccessor.requirePrincipal()).thenReturn(principal);
        when(resourceOwnershipService.resolveOwnerId(ResourceTypeCode.POST, 100L)).thenReturn(9L);

        RequireOwnerOrPermission ownership = annotationOf("ownerOnly", RequireOwnerOrPermission.class);

        assertDoesNotThrow(() -> authorizationService.authorize(null, null, ownership, 100L));
    }

    @Test
    void shouldRejectNonOwnerWithoutFallbackPermission() throws Exception {
        LoginPrincipal principal = new LoginPrincipal(9L, RoleCode.STUDENT, Set.of());
        when(currentUserAccessor.requirePrincipal()).thenReturn(principal);
        when(resourceOwnershipService.resolveOwnerId(ResourceTypeCode.POST, 100L)).thenReturn(10L);

        RequireOwnerOrPermission ownership = annotationOf("ownerOnly", RequireOwnerOrPermission.class);

        assertThrows(OwnershipDeniedException.class,
                () -> authorizationService.authorize(null, null, ownership, 100L));
    }

    private <A extends java.lang.annotation.Annotation> A annotationOf(String methodName, Class<A> type)
            throws Exception {
        Method method = Fixture.class.getDeclaredMethod(methodName);
        return method.getAnnotation(type);
    }

    private static class Fixture {

        @RequireRole(RoleCode.STUDENT)
        @RequirePermission(allOf = {"post:update:self"})
        void studentOnly() {
        }

        @RequireRole(RoleCode.ADMIN)
        void adminOnly() {
        }

        @RequireOwnerOrPermission(resource = ResourceTypeCode.POST, idParam = "postId")
        void ownerOnly() {
        }
    }
}
