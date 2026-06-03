package com.campus.campus_life_backend.common.security.interceptor;

import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.service.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.annotation.Annotation;
import java.util.Map;

public class RbacInterceptor implements HandlerInterceptor {

    private final AuthorizationService authorizationService;

    public RbacInterceptor(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireLogin requireLogin = findAnnotation(handlerMethod, RequireLogin.class);
        RequireRole requireRole = findAnnotation(handlerMethod, RequireRole.class);
        RequirePermission requirePermission = findAnnotation(handlerMethod, RequirePermission.class);
        RequireOwnerOrPermission requireOwnerOrPermission =
                findAnnotation(handlerMethod, RequireOwnerOrPermission.class);

        if (requireLogin != null) {
            authorizationService.requireAuthenticated();
        }

        Long resourceId = requireOwnerOrPermission != null
                ? resolveResourceId(request, requireOwnerOrPermission.idParam())
                : null;
        authorizationService.authorize(requireRole, requirePermission, requireOwnerOrPermission, resourceId);
        return true;
    }

    @Nullable
    private <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> annotationType) {
        A annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), annotationType);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), annotationType);
    }

    @Nullable
    private Long resolveResourceId(HttpServletRequest request, String idParam) {
        Object uriVariablesAttr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVariablesAttr instanceof Map<?, ?> uriVariables) {
            Object rawValue = uriVariables.get(idParam);
            Long parsed = parseLong(rawValue);
            if (parsed != null) {
                return parsed;
            }
        }
        return parseLong(request.getParameter(idParam));
    }

    @Nullable
    private Long parseLong(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
