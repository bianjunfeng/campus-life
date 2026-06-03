package com.campus.campus_life_backend.modules.auth.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.user.dto.UserDTO;
import com.campus.campus_life_backend.modules.auth.dto.LoginRequest;
import com.campus.campus_life_backend.modules.auth.dto.RegisterAppRequest;
import com.campus.campus_life_backend.modules.auth.dto.RegisterRequest;
import com.campus.campus_life_backend.modules.auth.dto.SendVerificationCodeRequest;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.auth.service.AuthService;
import com.campus.campus_life_backend.modules.auth.service.OAuthStateService;
import com.campus.campus_life_backend.modules.auth.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String WECHAT_PROVIDER = "wechat";
    private static final String QQ_PROVIDER = "qq";

    private final AuthService authService;
    private final VerificationService verificationService;
    private final OAuthStateService oauthStateService;
    private final CurrentUserAccessor currentUserAccessor;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          VerificationService verificationService,
                          OAuthStateService oauthStateService,
                          CurrentUserAccessor currentUserAccessor,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.verificationService = verificationService;
        this.oauthStateService = oauthStateService;
        this.currentUserAccessor = currentUserAccessor;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        // 隐藏敏感信息
        user.setPasswordHash(null);
        return ApiResponse.success(user);
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> registerUsers(@Valid @RequestBody RegisterAppRequest requestApp,
                                                          HttpServletRequest httpRequest) {
        // 前端调用 /auth/users，使用 app 专用请求体，统一做参数校验
        RegisterRequest request = new RegisterRequest();
        request.setPhone(requestApp.getPhone());
        request.setPassword(requestApp.getPassword());
        request.setCode(requestApp.getCode());
        request.setScene(requestApp.getScene());
        request.setUsername(requestApp.getNickName());

        // 学生信息（学生注册时）
        request.setSchool(requestApp.getSchool());
        request.setStudentNo(requestApp.getStudentId());
        request.setCollege(requestApp.getCollege());
        request.setMajor(requestApp.getMajor());
        request.setGrade(requestApp.getGrade());
        request.setClassName(requestApp.getClassName());
        request.setRealName(requestApp.getRealName());

        // 商家信息（商家注册时）
        request.setMerchantName(requestApp.getMerchantName());
        request.setMerchantTypeId(requestApp.getMerchantTypeId());
        request.setContactName(requestApp.getContactName());
        request.setContactPhone(requestApp.getContactPhone());
        request.setAddress(requestApp.getAddress());

        // 注册用户
        User user = authService.register(request);

        // 注册成功后自动生成token并返回
        Map<String, String> tokens = authService.generateTokensForLogin(user, request.getPhone(), "code", httpRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("token", tokens.get("accessToken")); // 兼容旧接口
        data.put("accessToken", tokens.get("accessToken"));
        data.put("refreshToken", tokens.get("refreshToken"));
        data.put("user", UserDTO.fromUser(user));

        return ApiResponse.success(data);
    }

    @PostMapping("/login")
    public ApiResponse<User> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = authService.login(request);
            authService.recordLoginSuccess(user, request, httpRequest);
            // 隐藏敏感信息
            user.setPasswordHash(null);
            return ApiResponse.success(user);
        } catch (BusinessException e) {
            authService.recordLoginFailure(request, e.getMessage(), httpRequest);
            throw e;
        }
    }

    @PostMapping("/tokens")
    public ApiResponse<Map<String, Object>> loginTokens(@Valid @RequestBody LoginRequest request,
                                                       HttpServletRequest httpRequest) {
        try {
            // 前端调用 /auth/tokens，返回token和用户信息
            User user = authService.login(request);
            if (user == null) {
                throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登录失败：用户信息为空");
            }

            if (user.getId() == null) {
                throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登录失败：用户ID为空");
            }

            // 生成token
            Map<String, String> tokens = authService.generateTokensForLogin(user, request, httpRequest);
            String accessToken = tokens.get("accessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登录失败：token生成失败");
            }

            // 转换为DTO
            UserDTO userDTO = UserDTO.fromUser(user);
            if (userDTO == null) {
                throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登录失败：用户信息转换失败");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("token", accessToken); // 兼容旧接口
            data.put("accessToken", accessToken);
            data.put("refreshToken", tokens.get("refreshToken"));
            data.put("user", userDTO);

            return ApiResponse.success(data);
        } catch (BusinessException e) {
            authService.recordLoginFailure(request, e.getMessage(), httpRequest);
            // 业务异常（如密码错误、验证码错误等）会被GlobalExceptionHandler处理
            throw e;
        } catch (Exception e) {
            authService.recordLoginFailure(request, "登录失败，请稍后重试", httpRequest);
            // 其他异常记录日志并返回错误
            logger.error("登录失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登录失败，请稍后重试", e);
        }
    }

    @PostMapping("/send-code")
    public ApiResponse<String> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request,
                                                    HttpServletRequest httpRequest) {
        verificationService.sendVerificationCode(request.getPhone(), request.getScene(), getClientIp(httpRequest));
        return ApiResponse.success("验证码发送成功");
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取微信登录授权URL
     */
    @GetMapping("/oauth/wechat/authorize")
    public ApiResponse<Map<String, String>> getWechatAuthUrl(@RequestParam String redirectUri) {
        try {
            String state = oauthStateService.createState(WECHAT_PROVIDER, redirectUri);
            String authUrl = authService.getWechatOAuthService().getAuthorizationUrl(redirectUri, state);
            Map<String, String> data = new HashMap<>();
            data.put("authUrl", authUrl);
            return ApiResponse.success(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取微信授权URL失败", e);
            throw new BusinessException(BusinessErrorCode.OAUTH_AUTHORIZE_URL_FAILED, "获取微信授权URL失败", e);
        }
    }

    /**
     * 微信OAuth回调登录
     */
    @GetMapping("/oauth/wechat/callback")
    public ApiResponse<Map<String, Object>> wechatCallback(@RequestParam String code,
                                                           @RequestParam(required = false) String state,
                                                           @RequestParam String redirectUri,
                                                           HttpServletRequest httpRequest) {
        try {
            if (!oauthStateService.validateAndConsumeState(WECHAT_PROVIDER, state, redirectUri)) {
                throw new BusinessException(BusinessErrorCode.OAUTH_STATE_INVALID);
            }
            User user = authService.loginWithWechat(code, redirectUri);
            Map<String, String> tokens = authService.generateTokensForLogin(user, null, WECHAT_PROVIDER, httpRequest);

            Map<String, Object> data = new HashMap<>();
            data.put("token", tokens.get("accessToken")); // 兼容旧接口
            data.put("accessToken", tokens.get("accessToken"));
            data.put("refreshToken", tokens.get("refreshToken"));
            data.put("user", UserDTO.fromUser(user));
            return ApiResponse.success(data);
        } catch (BusinessException e) {
            authService.recordOAuthFailure(WECHAT_PROVIDER, e.getMessage(), httpRequest);
            throw e;
        } catch (Exception e) {
            authService.recordOAuthFailure(WECHAT_PROVIDER, "微信登录失败，请稍后重试", httpRequest);
            logger.error("微信登录失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "微信登录失败，请稍后重试", e);
        }
    }

    /**
     * 获取QQ登录授权URL
     */
    @GetMapping("/oauth/qq/authorize")
    public ApiResponse<Map<String, String>> getQQAuthUrl(@RequestParam String redirectUri) {
        try {
            String state = oauthStateService.createState(QQ_PROVIDER, redirectUri);
            String authUrl = authService.getQQOAuthService().getAuthorizationUrl(redirectUri, state);
            Map<String, String> data = new HashMap<>();
            data.put("authUrl", authUrl);
            return ApiResponse.success(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取QQ授权URL失败", e);
            throw new BusinessException(BusinessErrorCode.OAUTH_AUTHORIZE_URL_FAILED, "获取QQ授权URL失败", e);
        }
    }

    /**
     * QQ OAuth回调登录
     */
    @GetMapping("/oauth/qq/callback")
    public ApiResponse<Map<String, Object>> qqCallback(@RequestParam String code,
                                                      @RequestParam(required = false) String state,
                                                      @RequestParam String redirectUri,
                                                      HttpServletRequest httpRequest) {
        try {
            if (!oauthStateService.validateAndConsumeState(QQ_PROVIDER, state, redirectUri)) {
                throw new BusinessException(BusinessErrorCode.OAUTH_STATE_INVALID);
            }
            User user = authService.loginWithQQ(code, redirectUri);
            Map<String, String> tokens = authService.generateTokensForLogin(user, null, QQ_PROVIDER, httpRequest);

            Map<String, Object> data = new HashMap<>();
            data.put("token", tokens.get("accessToken")); // 兼容旧接口
            data.put("accessToken", tokens.get("accessToken"));
            data.put("refreshToken", tokens.get("refreshToken"));
            data.put("user", UserDTO.fromUser(user));
            return ApiResponse.success(data);
        } catch (BusinessException e) {
            authService.recordOAuthFailure(QQ_PROVIDER, e.getMessage(), httpRequest);
            throw e;
        } catch (Exception e) {
            authService.recordOAuthFailure(QQ_PROVIDER, "QQ登录失败，请稍后重试", httpRequest);
            logger.error("QQ登录失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "QQ登录失败，请稍后重试", e);
        }
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "刷新令牌不能为空");
            }

            Map<String, String> tokens = authService.refreshTokens(refreshToken);

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", tokens.get("accessToken"));
            data.put("refreshToken", tokens.get("refreshToken"));

            return ApiResponse.success(data);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("刷新令牌失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "刷新令牌失败，请稍后重试", e);
        }
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @RequireLogin
    public ApiResponse<String> logout() {
        try {
            Long userId = currentUserAccessor.requireUserId();
            String accessToken = currentUserAccessor.getCurrentAccessToken();
            authService.logout(userId, accessToken);
            return ApiResponse.success("登出成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("登出失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登出失败，请稍后重试", e);
        }
    }

    @PostMapping("/logout-all")
    @RequireLogin
    public ApiResponse<String> logoutAll() {
        try {
            authService.logoutAll(currentUserAccessor.requireUserId());
            return ApiResponse.success("全部设备已退出");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("全部设备登出失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "全部设备登出失败，请稍后重试", e);
        }
    }

    @PostMapping("/logout-others")
    @RequireLogin
    public ApiResponse<String> logoutOthers() {
        try {
            Long userId = currentUserAccessor.requireUserId();
            String accessToken = currentUserAccessor.getCurrentAccessToken();
            authService.logoutOthers(userId, jwtUtil.getSessionIdFromToken(accessToken));
            return ApiResponse.success("其他设备已退出");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("退出其他设备失败", e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "退出其他设备失败，请稍后重试", e);
        }
    }

    @GetMapping("/sessions")
    @RequireLogin
    public ApiResponse<List<Map<String, Object>>> sessions() {
        return ApiResponse.success(authService.findUserSessions(currentUserAccessor.requireUserId()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @RequireLogin
    public ApiResponse<String> revokeSession(@PathVariable String sessionId) {
        try {
            authService.revokeUserSession(currentUserAccessor.requireUserId(), sessionId);
            return ApiResponse.success("会话已退出");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("退出指定会话失败: sessionId={}", sessionId, e);
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "退出指定会话失败，请稍后重试", e);
        }
    }
}
