package com.campus.campus_life_backend.common.config;

/**
 * ClassName: SecurityConfig
 * Description:
 *
 * @Author Junfeng Bian
 * @Create 2025/11/29 20:25
 * @Version 1.0
 */

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 提供一个空的 UserDetailsService，防止 Spring Boot 自动生成默认用户密码
     * 因为我们使用的是 JWT 认证，不需要 Spring Security 的默认用户认证
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UnsupportedOperationException("此应用使用 JWT 认证，不支持 Spring Security 默认用户认证");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            // 禁用CSRF保护，因为我们使用的是JWT或类似的无状态认证
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            // 公开查询接口放行，其余请求默认要求登录
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/error",
                    "/uploads/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/actuator/prometheus",
                    "/api/auth/**",
                    "/api/perf/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payment/alipay/notify", "/api/payment/wechat/notify").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payment/alipay/return").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/forum/posts/me",
                    "/api/forum/posts/following",
                    "/api/forum/posts/me/favorites",
                    "/api/users/me",
                    "/api/users/me/stats",
                    "/api/users/me/wallet",
                    "/api/users/me/auth-status/any",
                    "/api/vouchers/my-orders",
                    "/api/vouchers/my-orders/*",
                    "/api/vouchers/my-orders/order-no/*",
                    "/api/coupons/grab/result"
                ).authenticated()
                .requestMatchers(HttpMethod.GET,
                    "/api/categories",
                    "/api/categories/posts",
                    "/api/search/**",
                    "/api/welfare/home",
                    "/api/coupons",
                    "/api/coupons/*",
                    "/api/flash-sales",
                    "/api/products/*",
                    "/api/vouchers/available",
                    "/api/shops",
                    "/api/shops/types",
                    "/api/shops/nearby",
                    "/api/shops/*",
                    "/api/forum/posts",
                    "/api/forum/posts/user/*",
                    "/api/forum/posts/*",
                    "/api/forum/posts/*/comments",
                    "/api/users/*/followers",
                    "/api/users/*/followees",
                    "/api/users/*/mutual-follows"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // 设置会话为无状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        ApiResponse.error(BusinessErrorCode.LOGIN_REQUIRED.getCode(),
                                BusinessErrorCode.LOGIN_REQUIRED.getMessage()));
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        ApiResponse.error(BusinessErrorCode.FORBIDDEN.getCode(),
                                BusinessErrorCode.FORBIDDEN.getMessage()));
    }

    private void writeJsonResponse(HttpServletResponse response, int status, ApiResponse<?> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
