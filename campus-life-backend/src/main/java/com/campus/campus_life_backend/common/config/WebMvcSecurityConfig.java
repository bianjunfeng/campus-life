package com.campus.campus_life_backend.common.config;

import com.campus.campus_life_backend.common.security.service.AuthorizationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final AuthorizationService authorizationService;

    public WebMvcSecurityConfig(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new com.campus.campus_life_backend.common.security.interceptor.RbacInterceptor(
                authorizationService
        ));
    }
}
