package com.campus.campus_life_backend.common.security.aspect;

import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.service.AuthorizationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
@Component
public class RbacAspect {

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    private final AuthorizationService authorizationService;

    public RbacAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Around(
            "@annotation(com.campus.campus_life_backend.common.security.annotation.RequireLogin) || " +
            "@annotation(com.campus.campus_life_backend.common.security.annotation.RequireRole) || " +
            "@annotation(com.campus.campus_life_backend.common.security.annotation.RequirePermission) || " +
            "@annotation(com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission) || " +
            "@within(com.campus.campus_life_backend.common.security.annotation.RequireLogin) || " +
            "@within(com.campus.campus_life_backend.common.security.annotation.RequireRole) || " +
            "@within(com.campus.campus_life_backend.common.security.annotation.RequirePermission) || " +
            "@within(com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission)"
    )
    public Object authorize(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = resolveMethod(joinPoint);
        Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());

        RequireLogin requireLogin = findAnnotation(method, targetClass, RequireLogin.class);
        RequireRole requireRole = findAnnotation(method, targetClass, RequireRole.class);
        RequirePermission requirePermission = findAnnotation(method, targetClass, RequirePermission.class);
        RequireOwnerOrPermission requireOwnerOrPermission =
                findAnnotation(method, targetClass, RequireOwnerOrPermission.class);

        if (requireLogin != null) {
            authorizationService.requireAuthenticated();
        }

        Long resourceId = requireOwnerOrPermission != null
                ? resolveResourceId(method, joinPoint.getArgs(), requireOwnerOrPermission.idParam())
                : null;
        authorizationService.authorize(requireRole, requirePermission, requireOwnerOrPermission, resourceId);
        return joinPoint.proceed();
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return AopUtils.getMostSpecificMethod(method, AopUtils.getTargetClass(joinPoint.getTarget()));
    }

    @Nullable
    private <A extends Annotation> A findAnnotation(Method method, Class<?> targetClass, Class<A> annotationType) {
        A annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, annotationType);
    }

    @Nullable
    private Long resolveResourceId(Method method, Object[] args, String idParam) {
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (parameterNames == null) {
            return null;
        }
        for (int i = 0; i < parameterNames.length; i++) {
            if (idParam.equals(parameterNames[i])) {
                return parseLong(args[i]);
            }
        }
        return null;
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
