package com.campus.campus_life_backend.common.security.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class LoginPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final RoleCode roleCode;
    private final Set<String> permissions;

    public LoginPrincipal(Long userId, RoleCode roleCode, Set<String> permissions) {
        this.userId = userId;
        this.roleCode = roleCode == null ? RoleCode.GUEST : roleCode;
        this.permissions = Set.copyOf(new LinkedHashSet<>(permissions == null ? Set.of() : permissions));
    }

    public Long getUserId() {
        return userId;
    }

    public RoleCode getRoleCode() {
        return roleCode;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasRole(RoleCode expectedRole) {
        return roleCode == expectedRole;
    }

    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }

    public static LoginPrincipal guest(Long userId) {
        return new LoginPrincipal(userId, RoleCode.GUEST, Set.of());
    }

    @Override
    public String toString() {
        return "LoginPrincipal{" +
                "userId=" + userId +
                ", roleCode=" + roleCode +
                ", permissions=" + permissions +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleCode, permissions);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LoginPrincipal that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && roleCode == that.roleCode
                && Objects.equals(permissions, that.permissions);
    }
}
