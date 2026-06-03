package com.campus.campus_life_ai.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAiAuthorizationInterceptor adminAiAuthorizationInterceptor;
    private final MerchantAiAuthorizationInterceptor merchantAiAuthorizationInterceptor;

    public WebMvcConfig(AdminAiAuthorizationInterceptor adminAiAuthorizationInterceptor,
                        MerchantAiAuthorizationInterceptor merchantAiAuthorizationInterceptor) {
        this.adminAiAuthorizationInterceptor = adminAiAuthorizationInterceptor;
        this.merchantAiAuthorizationInterceptor = merchantAiAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAiAuthorizationInterceptor)
                .addPathPatterns("/api/admin/ai/**");
        registry.addInterceptor(merchantAiAuthorizationInterceptor)
                .addPathPatterns("/api/merchant/ai/**");
    }
}
