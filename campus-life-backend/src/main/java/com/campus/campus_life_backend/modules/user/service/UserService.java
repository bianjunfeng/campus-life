package com.campus.campus_life_backend.modules.user.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.entity.StudentProfile;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.user.dto.PublicUserProfileDTO;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserFollowMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import com.campus.campus_life_backend.modules.user.mapper.StudentProfileMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.search.event.UserSearchEventPublisher;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final UserFollowMapper userFollowMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserSearchService userSearchService;
    private final UserSearchEventPublisher userSearchEventPublisher;

    public record AdminUserStatusChange(Integer oldStatus, Integer newStatus) {
    }

    public UserService(UserMapper userMapper, PostMapper postMapper, UserFollowMapper userFollowMapper,
                      StudentProfileMapper studentProfileMapper, MerchantMapper merchantMapper,
                      PasswordEncoder passwordEncoder, TokenService tokenService,
                      ObjectProvider<UserSearchService> userSearchServiceProvider,
                      ObjectProvider<UserSearchEventPublisher> userSearchEventPublisherProvider) {
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.userFollowMapper = userFollowMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userSearchService = userSearchServiceProvider == null ? null : userSearchServiceProvider.getIfAvailable();
        this.userSearchEventPublisher = userSearchEventPublisherProvider == null ? null : userSearchEventPublisherProvider.getIfAvailable();
    }

    public User getUserById(Long id) {
        return userMapper.findById(id);
    }

    public User getCurrentUser(Long userId) {
        return userMapper.findById(userId);
    }

    @Transactional
    public AdminUserStatusChange adminUpdateStatus(Long userId, Integer status) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND);
        }
        Integer oldStatus = user.getStatus();
        userMapper.updateStatus(userId, status);
        if (!Integer.valueOf(1).equals(status)) {
            tokenService.revokeAllUserTokens(userId, AuthSessionService.STATUS_LOGGED_OUT);
        }
        if (status != null && status == 1) {
            syncUserSearch(userId);
        } else {
            deleteUserSearch(userId);
        }
        return new AdminUserStatusChange(oldStatus, status);
    }

    public PublicUserProfileDTO getPublicUserProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }
        PublicUserProfileDTO dto = PublicUserProfileDTO.fromUser(user);
        dto.setPostCount(postMapper.countByUserId(userId));
        dto.setFollowingCount(userFollowMapper.countFollowings(userId));
        dto.setFollowerCount(userFollowMapper.countFollowers(userId));
        return dto;
    }

    @Transactional
    public User updateProfile(Long userId, String username, String phone, String email, String avatarUrl, String bio,
                              Integer gender, java.time.LocalDate birthday, String region, String occupation) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND);
        }
        
        if (username != null) {
            user.setUsername(username);
        }
        if (phone != null) {
            String normalizedPhone = trimToNull(phone);
            if (normalizedPhone != null && !normalizedPhone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(BusinessErrorCode.CONTACT_PHONE_INVALID, "手机号格式不正确");
            }
            if (normalizedPhone != null) {
                User existing = userMapper.findByPhone(normalizedPhone);
                if (existing != null && !existing.getId().equals(userId)) {
                    throw new BusinessException(BusinessErrorCode.PHONE_REGISTERED);
                }
                user.setPhone(normalizedPhone);
            }
        }
        if (email != null) {
            String normalizedEmail = trimToNull(email);
            if (normalizedEmail != null && !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "邮箱格式不正确");
            }
            if (normalizedEmail != null) {
                User existing = userMapper.findByEmail(normalizedEmail);
                if (existing != null && !existing.getId().equals(userId)) {
                    throw new BusinessException(BusinessErrorCode.EMAIL_REGISTERED);
                }
            }
            user.setEmail(normalizedEmail);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        if (birthday != null) {
            user.setBirthday(birthday);
        }
        if (region != null) {
            user.setRegion(region);
        }
        if (occupation != null) {
            user.setOccupation(occupation);
        }
        
        userMapper.updateUser(user);
        syncUserSearch(user.getId());
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND);
        }
        if (isBlank(oldPassword) || isBlank(newPassword)) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "原密码和新密码不能为空");
        }
        String passwordHash = user.getPasswordHash();
        if (isBlank(passwordHash)) {
            throw new BusinessException(BusinessErrorCode.PASSWORD_NOT_SET);
        }
        if (!passwordHash.startsWith("$2a$") && !passwordHash.startsWith("$2b$") && !passwordHash.startsWith("$2y$")) {
            throw new BusinessException(BusinessErrorCode.PASSWORD_FORMAT_INVALID);
        }
        if (!passwordEncoder.matches(oldPassword, passwordHash)) {
            throw new BusinessException(BusinessErrorCode.PASSWORD_WRONG, "原密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "新密码不能与原密码相同");
        }
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$")) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "新密码至少8位，且需要同时包含字母和数字");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        tokenService.revokeAllUserTokens(userId, AuthSessionService.STATUS_LOGGED_OUT);
    }
    
    /**
     * 获取用户统计数据
     */
    public Map<String, Integer> getUserStats(Long userId) {
        Map<String, Integer> stats = new HashMap<>();
        
        // 统计帖子数量
        int postCount = postMapper.countByUserId(userId);
        stats.put("postCount", postCount);
        
        // 统计关注数量
        int followingCount = userFollowMapper.countFollowings(userId);
        stats.put("followingCount", followingCount);
        
        // 统计粉丝数量
        int followerCount = userFollowMapper.countFollowers(userId);
        stats.put("followerCount", followerCount);
        
        return stats;
    }
    
    /**
     * 检查用户是否已通过学生认证
     * 通过查询 student_profile 表，如果存在且 status=1，则认为已通过认证
     */
    public boolean isStudentVerified(Long userId) {
        try {
            // 查询已通过审核的学生档案（status=1）
            StudentProfile profile = studentProfileMapper.findByUserId(userId);
            boolean verified = profile != null && profile.getStatus() != null && profile.getStatus() == 1;
            logger.debug("检查学生认证状态: userId={}, verified={}", userId, verified);
            return verified;
        } catch (Exception e) {
            logger.error("检查学生认证状态失败: userId={}, error={}", userId, e.getMessage(), e);
            // 如果查询出错，返回false
            return false;
        }
    }
    
    /**
     * 检查用户是否已通过商家认证
     * 通过查询 merchant 表，如果存在且 status=1，则认为已通过认证
     */
    public boolean isMerchantVerified(Long userId) {
        try {
            Merchant merchant = merchantMapper.findByUserId(userId);
            boolean verified = merchant != null && merchant.getStatus() != null && merchant.getStatus() == 1;
            logger.debug("检查商家认证状态: userId={}, verified={}", userId, verified);
            return verified;
        } catch (Exception e) {
            logger.error("检查商家认证状态失败: userId={}, error={}", userId, e.getMessage(), e);
            // 如果查询出错，返回false
            return false;
        }
    }
    
    /**
     * 检查用户是否已通过学生或商家任一认证（二选一）
     * 只要通过了一个认证，就不应该显示任何认证入口
     */
    public boolean hasAnyAuthVerified(Long userId) {
        try {
            boolean studentVerified = isStudentVerified(userId);
            boolean merchantVerified = isMerchantVerified(userId);
            boolean hasAny = studentVerified || merchantVerified;
            logger.debug("检查用户是否已通过任一认证: userId={}, studentVerified={}, merchantVerified={}, hasAny={}", 
                    userId, studentVerified, merchantVerified, hasAny);
            return hasAny;
        } catch (Exception e) {
            logger.error("检查用户认证状态失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    public Map<String, Object> getCurrentAuthStatus(Long userId) {
        boolean studentVerified = isStudentVerified(userId);
        boolean merchantVerified = isMerchantVerified(userId);
        boolean hasAny = studentVerified || merchantVerified;

        Map<String, Object> result = new HashMap<>();
        result.put("hasAnyAuth", hasAny);
        result.put("studentVerified", studentVerified);
        result.put("merchantVerified", merchantVerified);
        result.put("shouldHideAllAuth", hasAny);

        StudentProfile studentProfile = studentProfileMapper.findByUserId(userId);
        if (studentProfile != null) {
            result.put("studentStatus", studentProfile.getStatus());
            result.put("studentStatusText", statusText(studentProfile.getStatus()));
            result.put("realName", studentProfile.getRealName());
            result.put("school", studentProfile.getSchool());
            result.put("college", studentProfile.getCollege());
            result.put("major", studentProfile.getMajor());
            result.put("grade", studentProfile.getGrade());
            result.put("className", studentProfile.getClassName());
            result.put("studentNo", studentProfile.getStudentNo());
        }

        Merchant merchant = merchantMapper.findByUserId(userId);
        if (merchant != null) {
            result.put("merchantId", merchant.getId());
            result.put("merchantStatus", merchant.getStatus());
            result.put("merchantStatusText", merchantStatusText(merchant.getStatus()));
            result.put("merchantName", merchant.getName());
            result.put("merchantAddress", merchant.getAddress());
            result.put("merchantContactName", merchant.getContactName());
            result.put("merchantContactPhone", merchant.getContactPhone());
        }

        return result;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "未知";
        };
    }

    private String merchantStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已冻结";
            case 3 -> "已关闭";
            default -> "未知";
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void syncUserSearch(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            if (userSearchEventPublisher != null) {
                userSearchEventPublisher.publishUpsert(userId, "user_profile_changed");
                return;
            }
            if (userSearchService != null) {
                userSearchService.syncUserToEs(userId);
            }
        } catch (Exception ignored) {
        }
    }

    private void deleteUserSearch(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            if (userSearchEventPublisher != null) {
                userSearchEventPublisher.publishDelete(userId, "user_status_changed");
                return;
            }
            if (userSearchService != null) {
                userSearchService.deleteUserFromEs(userId);
            }
        } catch (Exception ignored) {
        }
    }
}
