package com.campus.campus_life_backend.common.security.service;

import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.exception.OwnershipDeniedException;
import com.campus.campus_life_backend.common.security.exception.PermissionDeniedException;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.ownership.ResourceOwnershipService;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    private final CurrentUserAccessor currentUserAccessor;
    private final ResourceOwnershipService resourceOwnershipService;

    public AuthorizationService(CurrentUserAccessor currentUserAccessor,
                                ResourceOwnershipService resourceOwnershipService) {
        this.currentUserAccessor = currentUserAccessor;
        this.resourceOwnershipService = resourceOwnershipService;
    }

    public LoginPrincipal requireAuthenticated() {
        return currentUserAccessor.requirePrincipal();
    }

    public void authorize(@Nullable RequireRole requireRole,
                          @Nullable RequirePermission requirePermission,
                          @Nullable RequireOwnerOrPermission requireOwnerOrPermission,
                          @Nullable Long resourceId) {
        if (requireRole == null && requirePermission == null && requireOwnerOrPermission == null) {
            return;
        }

        LoginPrincipal principal = currentUserAccessor.requirePrincipal();

        if (requireRole != null) {
            requireAnyRole(principal, requireRole.value());
        }
        if (requirePermission != null) {
            requirePermissions(principal, requirePermission.anyOf(), requirePermission.allOf());
        }
        if (requireOwnerOrPermission != null) {
            requireOwnerOrPermission(principal, requireOwnerOrPermission, resourceId);
        }
    }

    public void requireAnyRole(LoginPrincipal principal, RoleCode[] requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return;
        }
        boolean matched = Arrays.stream(requiredRoles).anyMatch(principal::hasRole);
        if (!matched) {
            String message = Arrays.stream(requiredRoles)
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new PermissionDeniedException("缺少角色权限: " + message);
        }
    }

    public void requirePermissions(LoginPrincipal principal, String[] anyOfPermissions, String[] allOfPermissions) {
        Set<String> missingAll = new LinkedHashSet<>();
        if (allOfPermissions != null) {
            for (String permission : allOfPermissions) {
                if (!principal.hasPermission(permission)) {
                    missingAll.add(permission);
                }
            }
        }

        if (!missingAll.isEmpty()) {
            throw new PermissionDeniedException("缺少权限: " + String.join(", ", missingAll));
        }

        if (anyOfPermissions != null && anyOfPermissions.length > 0) {
            boolean matched = Arrays.stream(anyOfPermissions).anyMatch(principal::hasPermission);
            if (!matched) {
                throw new PermissionDeniedException("缺少任一权限: " + String.join(", ", anyOfPermissions));
            }
        }
    }

    public void requireOwnerOrPermission(LoginPrincipal principal,
                                         RequireOwnerOrPermission annotation,
                                         @Nullable Long resourceId) {
        if (resourceId == null) {
            throw new OwnershipDeniedException("无法解析资源归属，拒绝访问");
        }
        Long ownerId = resourceOwnershipService.resolveOwnerId(annotation.resource(), resourceId);
        if (ownerId == null) {
            return;
        }
        if (principal.getUserId() != null && principal.getUserId().equals(ownerId)) {
            return;
        }
        if (isEmpty(annotation.anyOf()) && isEmpty(annotation.allOf())) {
            throw new OwnershipDeniedException();
        }
        try {
            requirePermissions(principal, annotation.anyOf(), annotation.allOf());
        } catch (PermissionDeniedException ex) {
            throw new OwnershipDeniedException();
        }
    }

    private boolean isEmpty(String[] values) {
        return values == null || values.length == 0;
    }
}
