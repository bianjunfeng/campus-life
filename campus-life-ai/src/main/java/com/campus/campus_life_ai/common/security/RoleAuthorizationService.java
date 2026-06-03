package com.campus.campus_life_ai.common.security;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RoleAuthorizationService {

    private final CurrentUserAccessor currentUserAccessor;
    private final BackendUserProfileClient backendUserProfileClient;

    public RoleAuthorizationService(CurrentUserAccessor currentUserAccessor,
                                    BackendUserProfileClient backendUserProfileClient) {
        this.currentUserAccessor = currentUserAccessor;
        this.backendUserProfileClient = backendUserProfileClient;
    }

    public void requireRole(String expectedRole, String forbiddenMessage) {
        currentUserAccessor.requireUserId();

        String expected = normalizeRole(expectedRole);
        String role = normalizeRole(currentUserAccessor.getCurrentRole());
        if (expected.equals(role)) {
            return;
        }
        if (StringUtils.hasText(role)) {
            throw new ForbiddenException(forbiddenMessage);
        }

        String fallbackRole = normalizeRole(backendUserProfileClient.fetchCurrentUserRole(
                currentUserAccessor.requireAuthorizationHeader()
        ));
        if (expected.equals(fallbackRole)) {
            return;
        }
        if (StringUtils.hasText(fallbackRole)) {
            throw new ForbiddenException(forbiddenMessage);
        }
        throw new UnauthorizedException("无法确认当前用户身份，请重新登录后重试");
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return switch (normalized) {
            case "0" -> "STUDENT";
            case "1" -> "MERCHANT";
            case "2" -> "ADMIN";
            default -> normalized;
        };
    }
}
