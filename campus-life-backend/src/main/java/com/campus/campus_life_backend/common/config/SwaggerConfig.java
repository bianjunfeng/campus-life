package com.campus.campus_life_backend.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI配置类
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "校园生活平台API文档",
                description = "校园生活平台后端API接口文档",
                version = "1.0.0",
                contact = @Contact(
                        name = "校园生活平台开发团队",
                        email = "admin@campuslife.com"
                )
        ),
        servers = @Server(url = "http://localhost:8080", description = "本地开发服务器")
)
@SecuritySchemes({
        @SecurityScheme(
                name = "Bearer Authentication",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "使用JWT令牌进行身份验证"
        )
})
public class SwaggerConfig {
    // 配置类内容，可根据需要添加更多配置
}

