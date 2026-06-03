package com.campus.campus_life_backend.modules.auth.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.common.util.JwtUtil;
import com.campus.campus_life_backend.modules.auth.dto.LoginRequest;
import com.campus.campus_life_backend.modules.auth.dto.RegisterRequest;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.user.entity.StudentProfile;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.search.event.UserSearchEventPublisher;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import com.campus.campus_life_backend.modules.user.mapper.StudentProfileMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import com.campus.campus_life_backend.modules.auth.service.impl.WechatOAuthService;
import com.campus.campus_life_backend.modules.auth.service.impl.QQOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final StudentProfileMapper studentProfileMapper;
    private final MerchantMapper merchantMapper;
    private final WechatOAuthService wechatOAuthService;
    private final QQOAuthService qqOAuthService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final AuthSessionService authSessionService;
    private final LoginAuditLogService loginAuditLogService;
    private final AdminAuthUtil adminAuthUtil;
    private final UserSearchService userSearchService;
    private final UserSearchEventPublisher userSearchEventPublisher;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                      VerificationService verificationService, StudentProfileMapper studentProfileMapper,
                      MerchantMapper merchantMapper,
                      WechatOAuthService wechatOAuthService, QQOAuthService qqOAuthService,
                      JwtUtil jwtUtil, TokenService tokenService,
                      AuthSessionService authSessionService,
                      LoginAuditLogService loginAuditLogService,
                      AdminAuthUtil adminAuthUtil,
                      ObjectProvider<UserSearchService> userSearchServiceProvider,
                      ObjectProvider<UserSearchEventPublisher> userSearchEventPublisherProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.studentProfileMapper = studentProfileMapper;
        this.merchantMapper = merchantMapper;
        this.wechatOAuthService = wechatOAuthService;
        this.qqOAuthService = qqOAuthService;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        this.authSessionService = authSessionService;
        this.loginAuditLogService = loginAuditLogService;
        this.adminAuthUtil = adminAuthUtil;
        this.userSearchService = userSearchServiceProvider == null ? null : userSearchServiceProvider.getIfAvailable();
        this.userSearchEventPublisher = userSearchEventPublisherProvider == null ? null : userSearchEventPublisherProvider.getIfAvailable();
    }

    @Transactional
    public User register(RegisterRequest req) {
        // 1. 验证验证码
        if (req.getCode() == null || req.getCode().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.CAPTCHA_REQUIRED);
        }
        if (!verificationService.verifyCode(req.getPhone(), req.getCode(), VerificationScene.REGISTER.code())) {
            throw new BusinessException(BusinessErrorCode.CAPTCHA_INVALID);
        }

        // 2. 重复校验
        if (req.getPhone() != null && !req.getPhone().isEmpty()
                && userMapper.findByPhone(req.getPhone()) != null) {
            throw new BusinessException(BusinessErrorCode.PHONE_REGISTERED);
        }
        if (req.getEmail() != null && !req.getEmail().isEmpty()
                && userMapper.findByEmail(req.getEmail()) != null) {
            throw new BusinessException(BusinessErrorCode.EMAIL_REGISTERED);
        }
        if (userMapper.findByUsername(req.getUsername()) != null) {
            throw new BusinessException(BusinessErrorCode.USERNAME_REGISTERED);
        }

        // 3. 判断注册类型：学生或商家
        boolean isStudent = req.getSchool() != null && !req.getSchool().isEmpty()
                && req.getStudentNo() != null && !req.getStudentNo().isEmpty();
        boolean isMerchant = req.getMerchantName() != null && !req.getMerchantName().isEmpty()
                && req.getMerchantTypeId() != null;

        // 如果既不是学生也不是商家，默认按学生处理
        if (!isStudent && !isMerchant) {
            throw new BusinessException(BusinessErrorCode.REGISTER_PROFILE_REQUIRED);
        }

        // 如果同时提供了学生和商家信息，优先按学生处理
        if (isStudent && isMerchant) {
            throw new BusinessException(BusinessErrorCode.REGISTER_ROLE_CONFLICT);
        }

        // 学生注册：检查学号是否已被使用
        if (isStudent) {
            StudentProfile existing = studentProfileMapper.findBySchoolAndStudentNo(
                req.getSchool(), req.getStudentNo());
            if (existing != null) {
                throw new BusinessException(BusinessErrorCode.STUDENT_NO_REGISTERED);
            }
        }

        // 商家注册：检查商家名称是否已被使用（可选，根据业务需求）
        if (isMerchant) {
            // 可以添加商家名称唯一性检查
            if (req.getContactPhone() != null && !req.getContactPhone().isEmpty()) {
                // 检查联系电话格式
                if (!req.getContactPhone().matches("^1[3-9]\\d{9}$")) {
                    throw new BusinessException(BusinessErrorCode.CONTACT_PHONE_INVALID);
                }
            }
        }

        // 4. 创建用户
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(isMerchant ? 1 : 0);  // 1-商家;0-学生
        user.setStatus(1);  // 正常状态
        user.setGender(0);  // 默认性别：0-保密

        userMapper.insertUser(user);

        // 5. 创建学生档案或商家信息
        if (isStudent) {
            // 创建学生档案
            StudentProfile profile = new StudentProfile();
            profile.setUserId(user.getId());
            profile.setRealName(req.getRealName());
            profile.setSchool(req.getSchool());
            profile.setCollege(req.getCollege());
            profile.setMajor(req.getMajor());
            profile.setGrade(req.getGrade());
            profile.setClassName(req.getClassName());
            profile.setStudentNo(req.getStudentNo());
            profile.setStatus(0);  // 待审核状态（新注册的学生档案需要审核）

            studentProfileMapper.insertStudentProfile(profile);
        } else if (isMerchant) {
            // 创建商家信息
            Merchant merchant = new Merchant();
            merchant.setUserId(user.getId());
            merchant.setName(req.getMerchantName());
            merchant.setTypeId(req.getMerchantTypeId());
            merchant.setContactName(req.getContactName());
            merchant.setContactPhone(req.getContactPhone());
            merchant.setAddress(req.getAddress());
            merchant.setStatus(0);  // 0-待审核

            merchantMapper.insertMerchant(merchant);
        }

        syncUserSearch(user.getId());
        return user;
    }

    public User login(LoginRequest req) {
        String account = req.getPhone() == null ? "" : req.getPhone().trim();
        User user = findUserByAccount(account);

        // 如果用户不存在，抛出异常，前端可以捕获并跳转到注册页面
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND_REGISTER_FIRST);
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.ACCOUNT_BANNED);
        }

        // 使用验证码登录
        if (req.getCode() != null && !req.getCode().isEmpty()) {
            if (!account.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(BusinessErrorCode.CAPTCHA_LOGIN_PHONE_ONLY);
            }
            if (verificationService.verifyCode(account, req.getCode(), VerificationScene.LOGIN.code())) {
                return user;
            } else {
                throw new BusinessException(BusinessErrorCode.CAPTCHA_INVALID);
            }
        }
        // 使用密码登录
        else if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            // 检查密码哈希是否存在且为有效格式
            String passwordHash = user.getPasswordHash();
            if (passwordHash == null || passwordHash.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.PASSWORD_NOT_SET);
            }

            // 检查是否是 BCrypt 格式（BCrypt 哈希通常以 $2a$, $2b$ 或 $2y$ 开头）
            if (!passwordHash.startsWith("$2a$") && !passwordHash.startsWith("$2b$")
                    && !passwordHash.startsWith("$2y$")) {
                throw new BusinessException(BusinessErrorCode.PASSWORD_FORMAT_INVALID);
            }

            if (passwordEncoder.matches(req.getPassword(), passwordHash)) {
                return user;
            } else {
                throw new BusinessException(BusinessErrorCode.PASSWORD_WRONG);
            }
        } else {
            throw new BusinessException(BusinessErrorCode.LOGIN_CREDENTIAL_REQUIRED);
        }
    }

    private User findUserByAccount(String account) {
        if (account == null || account.isEmpty()) {
            return null;
        }

        if (account.matches("^1[3-9]\\d{9}$")) {
            return userMapper.findByPhone(account);
        }

        if (account.contains("@")) {
            User userByEmail = userMapper.findByEmail(account);
            if (userByEmail != null) {
                return userByEmail;
            }
        }

        User userByUsername = userMapper.findByUsername(account);
        if (userByUsername != null) {
            return userByUsername;
        }

        return userMapper.findByPhone(account);
    }

    /**
     * 生成访问令牌和刷新令牌
     * @param userId 用户ID
     * @return 包含 accessToken 和 refreshToken 的 Map
     */
    public Map<String, String> generateTokens(Long userId) {
        return generateTokens(userId, newSessionId());
    }

    public Map<String, String> generateTokens(Long userId, String sessionId) {
        tokenService.requireTokenStoreAvailable();

        String effectiveSessionId = (sessionId == null || sessionId.isBlank()) ? newSessionId() : sessionId;
        RoleCode roleCode = resolveRoleCode(userId);
        String accessToken = jwtUtil.generateAccessToken(userId, roleCode, effectiveSessionId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, roleCode, effectiveSessionId);

        // 计算访问令牌的过期时间（秒）
        long accessTokenExpiration = jwtUtil.getExpirationDateFromToken(accessToken) != null ?
                (jwtUtil.getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis()) / 1000 : 1800;

        // 存储到 Redis
        tokenService.storeTokens(userId, effectiveSessionId, accessToken, refreshToken, accessTokenExpiration);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("sessionId", effectiveSessionId);
        return tokens;
    }

    public Map<String, String> generateTokensForLogin(User user,
                                                      String account,
                                                      String loginType,
                                                      HttpServletRequest request) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "登录失败：用户信息为空");
        }
        Map<String, String> tokens = generateTokens(user.getId());
        String sessionId = tokens.get("sessionId");
        String ip = getClientIp(request);
        String userAgent = getUserAgent(request);
        authSessionService.createOrUpdateSession(
                user,
                sessionId,
                tokens.get("accessToken"),
                tokens.get("refreshToken"),
                resolveDevice(userAgent),
                ip,
                userAgent
        );
        loginAuditLogService.recordSuccess(user, account, loginType, ip, userAgent, sessionId);
        return tokens;
    }

    public Map<String, String> generateTokensForLogin(User user, LoginRequest loginRequest, HttpServletRequest request) {
        return generateTokensForLogin(user, normalizeAccount(loginRequest), resolveLoginType(loginRequest), request);
    }

    public void recordLoginSuccess(User user, LoginRequest loginRequest, HttpServletRequest request) {
        loginAuditLogService.recordSuccess(
                user,
                normalizeAccount(loginRequest),
                resolveLoginType(loginRequest),
                getClientIp(request),
                getUserAgent(request),
                null
        );
    }

    public void recordLoginFailure(LoginRequest loginRequest, String failureReason, HttpServletRequest request) {
        loginAuditLogService.recordFailure(
                normalizeAccount(loginRequest),
                resolveLoginType(loginRequest),
                failureReason,
                getClientIp(request),
                getUserAgent(request)
        );
    }

    public void recordOAuthFailure(String provider, String failureReason, HttpServletRequest request) {
        loginAuditLogService.recordFailure(null, provider, failureReason, getClientIp(request), getUserAgent(request));
    }

    /**
     * 生成访问令牌（兼容旧接口）
     * @deprecated 建议使用 generateTokens 方法
     */
    @Deprecated
    public String generateToken(Long userId) {
        tokenService.requireTokenStoreAvailable();

        RoleCode roleCode = resolveRoleCode(userId);
        String sessionId = newSessionId();
        String accessToken = jwtUtil.generateAccessToken(userId, roleCode, sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, roleCode, sessionId);

        long accessTokenExpiration = jwtUtil.getExpirationDateFromToken(accessToken) != null ?
                (jwtUtil.getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis()) / 1000 : 1800;

        tokenService.storeTokens(userId, sessionId, accessToken, refreshToken, accessTokenExpiration);
        return accessToken;
    }

    /**
     * 从token中获取userId
     */
    public Long getUserIdFromToken(String token) {
        // 先验证 token 是否有效
        if (!jwtUtil.validateToken(token)) {
            return null;
        }

        // 检查是否在黑名单中
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId != null && !tokenService.isAccessTokenValid(userId, token)) {
            return null;
        }

        return userId;
    }

    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }

        return tokenService.isAccessTokenValid(userId, token);
    }

    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌和刷新令牌
     */
    public Map<String, String> refreshTokens(String refreshToken) {
        String refreshJti = null;
        boolean lockAcquired = false;

        try {
            tokenService.requireTokenStoreAvailable();

            // 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new BusinessException(BusinessErrorCode.REFRESH_TOKEN_INVALID);
            }

            String tokenType = jwtUtil.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                throw new BusinessException(BusinessErrorCode.REFRESH_TOKEN_TYPE_INVALID);
            }

            Long userId = jwtUtil.getUserIdFromToken(refreshToken);
            if (userId == null) {
                throw new BusinessException(BusinessErrorCode.REFRESH_TOKEN_USER_ID_MISSING);
            }

            refreshJti = jwtUtil.getJwtIdFromToken(refreshToken);
            if (refreshJti == null || refreshJti.isBlank()) {
                throw new BusinessException(BusinessErrorCode.REFRESH_TOKEN_INVALID);
            }

            String sessionId = jwtUtil.getSessionIdFromToken(refreshToken);
            if (tokenService.isRefreshTokenUsed(refreshJti)) {
                handleRefreshTokenReuse(userId);
            }

            if (!tokenService.tryAcquireRefreshLock(refreshJti, buildRefreshLockOwner(userId, sessionId))) {
                throw new BusinessException(BusinessErrorCode.REFRESH_TOKEN_EXPIRED);
            }
            lockAcquired = true;

            if (tokenService.isRefreshTokenUsed(refreshJti)) {
                handleRefreshTokenReuse(userId);
            }

            String oldAccessToken = tokenService.consumeRefreshToken(userId, refreshToken);
            if (oldAccessToken == null || oldAccessToken.isBlank()) {
                handleRefreshTokenReuse(userId);
            }

            tokenService.markRefreshTokenUsed(refreshJti, userId, sessionId, getRemainingTokenSeconds(refreshToken));
            tokenService.blacklistAccessTokenForRefresh(userId, oldAccessToken, getRemainingTokenSeconds(oldAccessToken));

            // 生成新的令牌对
            Map<String, String> tokens = generateTokens(userId, sessionId);
            authSessionService.updateTokenPair(tokens.get("sessionId"), tokens.get("accessToken"), tokens.get("refreshToken"));
            return tokens;
        } catch (BusinessException e) {
            throw toRefreshFailClosedException(e);
        } finally {
            if (lockAcquired) {
                tokenService.releaseRefreshLock(refreshJti);
            }
        }
    }

    /**
     * 登出，将访问令牌加入黑名单
     */
    public void logout(Long userId, String accessToken) {
        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            long expiration = jwtUtil.getExpirationDateFromToken(accessToken) != null ?
                    (jwtUtil.getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis()) / 1000 : 0;
            String sessionId = jwtUtil.getSessionIdFromToken(accessToken);
            if (sessionId != null && !sessionId.isBlank()) {
                tokenService.revokeSession(userId, sessionId, AuthSessionService.STATUS_LOGGED_OUT);
                tokenService.revokeTokenPair(userId, accessToken);
                if (expiration > 0) {
                    tokenService.blacklistAccessToken(userId, accessToken, expiration);
                }
                return;
            }

            tokenService.revokeTokenPair(userId, accessToken);
            if (expiration > 0) {
                tokenService.blacklistAccessToken(userId, accessToken, expiration);
            }
        }
    }

    public void logoutAll(Long userId) {
        tokenService.revokeAllUserTokens(userId, AuthSessionService.STATUS_LOGGED_OUT);
    }

    public void logoutOthers(Long userId, String currentSessionId) {
        if (currentSessionId == null || currentSessionId.isBlank()) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "当前会话ID缺失，请重新登录");
        }
        tokenService.revokeOtherUserSessions(userId, currentSessionId, AuthSessionService.STATUS_LOGGED_OUT);
    }

    public List<Map<String, Object>> findUserSessions(Long userId) {
        return authSessionService.findUserSessions(userId);
    }

    public void revokeUserSession(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "会话ID不能为空");
        }
        if (!authSessionService.sessionBelongsToUser(sessionId, userId)) {
            throw new BusinessException(BusinessErrorCode.FORBIDDEN, "无权操作该会话");
        }
        tokenService.revokeSession(userId, sessionId, AuthSessionService.STATUS_LOGGED_OUT);
    }

    public void forceLogoutUser(Long userId) {
        tokenService.revokeAllUserTokens(userId, AuthSessionService.STATUS_FORCE_LOGOUT);
    }

    /**
     * 微信OAuth登录
     */
    @Transactional
    public User loginWithWechat(String code, String redirectUri) {
        String accessToken = wechatOAuthService.getAccessToken(code, redirectUri);
        OAuthService.OAuthUserInfo userInfo = wechatOAuthService.getUserInfo(accessToken);

        // 查找是否已存在该微信用户
        User user = userMapper.findByWechatOpenid(userInfo.getOpenid());

        if (user == null) {
            // 新用户，自动注册
            user = new User();
            user.setWechatOpenid(userInfo.getOpenid());
            user.setUsername(userInfo.getNickname() != null ? userInfo.getNickname() : "微信用户");
            user.setAvatarUrl(userInfo.getAvatar());
            user.setRole(0);  // 默认学生角色
            user.setStatus(1);
            user.setGender(0);  // 默认性别：0-保密
            // 手机号可以为空，因为第三方登录
            user.setPhone("");
            user.setPasswordHash("");  // 第三方登录不需要密码

            userMapper.insertUser(user);
        } else {
            // 更新用户信息
            if (userInfo.getNickname() != null) {
                user.setUsername(userInfo.getNickname());
            }
            if (userInfo.getAvatar() != null) {
                user.setAvatarUrl(userInfo.getAvatar());
            }
            userMapper.updateUser(user);
        }

        syncUserSearch(user.getId());
        return user;
    }

    /**
     * QQ OAuth登录
     */
    @Transactional
    public User loginWithQQ(String code, String redirectUri) {
        String accessToken = qqOAuthService.getAccessToken(code, redirectUri);
        OAuthService.OAuthUserInfo userInfo = qqOAuthService.getUserInfo(accessToken);

        // 查找是否已存在该QQ用户
        User user = userMapper.findByQqOpenid(userInfo.getOpenid());

        if (user == null) {
            // 新用户，自动注册
            user = new User();
            user.setQqOpenid(userInfo.getOpenid());
            user.setUsername(userInfo.getNickname() != null ? userInfo.getNickname() : "QQ用户");
            user.setAvatarUrl(userInfo.getAvatar());
            user.setRole(0);  // 默认学生角色
            user.setStatus(1);
            user.setGender(0);  // 默认性别：0-保密
            // 手机号可以为空，因为第三方登录
            user.setPhone("");
            user.setPasswordHash("");  // 第三方登录不需要密码

            userMapper.insertUser(user);
        } else {
            // 更新用户信息
            if (userInfo.getNickname() != null) {
                user.setUsername(userInfo.getNickname());
            }
            if (userInfo.getAvatar() != null) {
                user.setAvatarUrl(userInfo.getAvatar());
            }
            userMapper.updateUser(user);
        }

        syncUserSearch(user.getId());
        return user;
    }

    /**
     * 获取微信OAuth服务（供Controller使用）
     */
    public WechatOAuthService getWechatOAuthService() {
        return wechatOAuthService;
    }

    /**
     * 获取QQ OAuth服务（供Controller使用）
     */
    public QQOAuthService getQQOAuthService() {
        return qqOAuthService;
    }

    private void syncUserSearch(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            if (userSearchEventPublisher != null) {
                userSearchEventPublisher.publishUpsert(userId, "auth_user_changed");
                return;
            }
            if (userSearchService != null) {
                userSearchService.syncUserToEs(userId);
            }
        } catch (Exception ignored) {
        }
    }

    private String resolveLoginType(LoginRequest request) {
        if (request != null && request.getCode() != null && !request.getCode().isBlank()) {
            return "code";
        }
        return "password";
    }

    private String normalizeAccount(LoginRequest request) {
        return request == null || request.getPhone() == null ? null : request.getPhone().trim();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return adminAuthUtil.getClientIp(request);
    }

    private String getUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String resolveDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "mobile";
        }
        return "web";
    }

    private String newSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private RoleCode resolveRoleCode(Long userId) {
        if (userId == null) {
            return RoleCode.GUEST;
        }
        User user = userMapper.findById(userId);
        return user == null ? RoleCode.GUEST : RoleCode.fromDbRole(user.getRole());
    }

    private long getRemainingTokenSeconds(String token) {
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);
        if (expirationDate == null) {
            return 0;
        }
        return Math.max(0, (expirationDate.getTime() - System.currentTimeMillis()) / 1000);
    }

    private String buildRefreshLockOwner(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return String.valueOf(userId);
        }
        return userId + ":" + sessionId;
    }

    private void handleRefreshTokenReuse(Long userId) {
        tokenService.revokeAllUserTokens(userId);
        try {
            loginAuditLogService.recordFailure(
                    userId == null ? null : String.valueOf(userId),
                    "refresh",
                    "REFRESH_TOKEN_REUSE",
                    null,
                    null
            );
        } catch (Exception ignored) {
        }
        throw new BusinessException(BusinessErrorCode.REFRESH_TOKEN_REUSED);
    }

    private BusinessException toRefreshFailClosedException(BusinessException e) {
        if (e.getCode() == BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE.getCode()) {
            return new BusinessException(
                    BusinessErrorCode.REFRESH_TOKEN_EXPIRED,
                    "刷新令牌已失效，请重新登录",
                    e
            );
        }
        return e;
    }
}
