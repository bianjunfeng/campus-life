package com.campus.campus_life_backend.modules.presence.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.presence.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/presence")
@RequireLogin
public class PresenceController {

    private final PresenceService presenceService;
    private final CurrentUserAccessor currentUserAccessor;
    private final JwtUtil jwtUtil;
    private final AdminAuthUtil adminAuthUtil;

    public PresenceController(PresenceService presenceService,
                              CurrentUserAccessor currentUserAccessor,
                              JwtUtil jwtUtil,
                              AdminAuthUtil adminAuthUtil) {
        this.presenceService = presenceService;
        this.currentUserAccessor = currentUserAccessor;
        this.jwtUtil = jwtUtil;
        this.adminAuthUtil = adminAuthUtil;
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Map<String, Object>> heartbeat(@RequestBody(required = false) Map<String, Object> body,
                                                      HttpServletRequest request) {
        LoginPrincipal principal = currentUserAccessor.requirePrincipal();
        presenceService.markOnline(
                principal,
                requireSessionId(),
                body == null ? null : stringValue(body.get("portal")),
                adminAuthUtil.getClientIp(request),
                request.getHeader("User-Agent")
        );
        return ApiResponse.success(Map.of("online", true, "ttlSeconds", PresenceService.ONLINE_TTL_SECONDS));
    }

    @PostMapping("/offline")
    public ApiResponse<Map<String, Object>> offline() {
        LoginPrincipal principal = currentUserAccessor.requirePrincipal();
        presenceService.markOffline(principal.getUserId(), requireSessionId());
        return ApiResponse.success(Map.of("offline", true));
    }

    private String requireSessionId() {
        String accessToken = currentUserAccessor.getCurrentAccessToken();
        String sessionId = accessToken == null ? null : jwtUtil.getSessionIdFromToken(accessToken);
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "当前会话ID缺失，请重新登录");
        }
        return sessionId;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
