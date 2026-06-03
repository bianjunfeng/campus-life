package com.campus.campus_life_backend.common.security.support;

import com.campus.campus_life_backend.common.security.exception.UnauthenticatedException;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserAccessor {

    @Nullable
    public LoginPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginPrincipal loginPrincipal) {
            return loginPrincipal;
        }
        if (principal instanceof Number number) {
            return LoginPrincipal.guest(number.longValue());
        }
        if (principal instanceof String text && !"anonymousUser".equalsIgnoreCase(text)) {
            try {
                return LoginPrincipal.guest(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public LoginPrincipal requirePrincipal() {
        LoginPrincipal principal = getCurrentPrincipal();
        if (principal == null) {
            throw new UnauthenticatedException();
        }
        return principal;
    }

    @Nullable
    public Long getCurrentUserId() {
        LoginPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.getUserId() : null;
    }

    public Long requireUserId() {
        return requirePrincipal().getUserId();
    }

    @Nullable
    public String getCurrentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object credentials = authentication.getCredentials();
        if (credentials instanceof String token && !token.isBlank()) {
            return token;
        }
        return null;
    }
}
