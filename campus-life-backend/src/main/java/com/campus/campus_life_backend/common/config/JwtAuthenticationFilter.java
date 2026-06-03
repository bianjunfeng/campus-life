package com.campus.campus_life_backend.common.config;

import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.PermissionCode;
import com.campus.campus_life_backend.common.security.service.LoginPrincipalFactory;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JWT 认证过滤器，将有效令牌转换为 Spring Security 认证上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final LoginPrincipalFactory loginPrincipalFactory;
    private final AuthSessionService authSessionService;
    private final ConcurrentMap<String, Long> lastSeenUpdateMillis = new ConcurrentHashMap<>();

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   TokenService tokenService,
                                   LoginPrincipalFactory loginPrincipalFactory,
                                   AuthSessionService authSessionService) {
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.loginPrincipalFactory = loginPrincipalFactory;
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null
                && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authorization.substring(7);
            LoginPrincipal principal = resolvePrincipal(token);
            if (principal != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                token,
                                buildAuthorities(principal)
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                touchLastSeenIfNeeded(token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private LoginPrincipal resolvePrincipal(String token) {
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        String tokenType = jwtUtil.getTokenType(token);
        if (!"access".equals(tokenType)) {
            return null;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return null;
        }
        if (!tokenService.isAccessTokenValid(userId, token)) {
            return null;
        }
        return loginPrincipalFactory.create(userId);
    }

    private void touchLastSeenIfNeeded(String token) {
        String sessionId = jwtUtil.getSessionIdFromToken(token);
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastSeenUpdateMillis.get(sessionId);
        if (last != null && now - last < 60_000L) {
            return;
        }
        lastSeenUpdateMillis.put(sessionId, now);
        try {
            authSessionService.touchLastSeen(sessionId);
        } catch (Exception ignored) {
        }
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(LoginPrincipal principal) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(principal.getRoleCode().authority()));
        for (String permission : principal.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(PermissionCode.toAuthority(permission)));
        }
        return authorities;
    }
}
