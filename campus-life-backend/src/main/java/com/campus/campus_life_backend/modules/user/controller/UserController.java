package com.campus.campus_life_backend.modules.user.controller;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.user.dto.PublicUserProfileDTO;
import com.campus.campus_life_backend.modules.user.dto.UserDTO;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.service.UserFollowService;
import com.campus.campus_life_backend.modules.user.service.UserService;
import com.campus.campus_life_backend.modules.user.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserAccessor currentUserAccessor;
    private final UserService userService;
    private final UserFollowService userFollowService;
    private final WalletService walletService;

    public UserController(CurrentUserAccessor currentUserAccessor,
                          UserService userService,
                          UserFollowService userFollowService,
                          WalletService walletService) {
        this.currentUserAccessor = currentUserAccessor;
        this.userService = userService;
        this.userFollowService = userFollowService;
        this.walletService = walletService;
    }

    @GetMapping("/me")
    @RequirePermission(anyOf = {"user:profile:read:self"})
    public ApiResponse<UserDTO> getCurrentUser() {
        Long userId = currentUserAccessor.requireUserId();
        User user = userService.getCurrentUser(userId);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND);
        }
        return ApiResponse.success(UserDTO.fromUser(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicUserProfileDTO> getPublicUserProfile(@PathVariable Long id) {
        PublicUserProfileDTO profile = userService.getPublicUserProfile(id);
        if (profile == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND);
        }

        Long currentUserId = currentUserAccessor.getCurrentUserId();
        boolean isFollowing = currentUserId != null
                && !currentUserId.equals(id)
                && userFollowService.isFollowing(currentUserId, id);
        profile.setIsFollowing(isFollowing);
        return ApiResponse.success(profile);
    }

    @PutMapping("/me/profile")
    @RequirePermission(anyOf = {"user:profile:update:self"})
    public ApiResponse<UserDTO> updateProfile(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();

        String username = (String) request.get("nickName");
        String phone = (String) request.get("phone");
        String email = (String) request.get("email");
        String avatarUrl = (String) request.get("icon");
        String bio = (String) request.get("intro");
        String region = (String) request.get("region");
        String occupation = (String) request.get("occupation");
        Integer gender = null;
        Object genderObj = request.get("gender");
        if (genderObj instanceof Number) {
            int parsed = ((Number) genderObj).intValue();
            if (parsed >= 0 && parsed <= 2) {
                gender = parsed;
            }
        } else if (genderObj instanceof String) {
            String genderStr = ((String) genderObj).trim();
            if ("0".equals(genderStr) || "保密".equals(genderStr)) {
                gender = 0;
            } else if ("1".equals(genderStr) || "男".equals(genderStr)) {
                gender = 1;
            } else if ("2".equals(genderStr) || "女".equals(genderStr)) {
                gender = 2;
            }
        }

        java.time.LocalDate birthday = null;
        Object birthdayObj = request.get("birthday");
        if (birthdayObj instanceof String birthdayStr && !birthdayStr.isEmpty()) {
            try {
                birthday = java.time.LocalDate.parse(birthdayStr);
            } catch (Exception ignored) {
            }
        }

        User user = userService.updateProfile(userId, username, phone, email, avatarUrl, bio, gender, birthday, region, occupation);
        return ApiResponse.success(UserDTO.fromUser(user));
    }

    @PutMapping("/me/password")
    @RequirePermission(anyOf = {"user:profile:update:self"})
    public ApiResponse<Void> changePassword(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        String oldPassword = (String) request.get("oldPassword");
        String newPassword = (String) request.get("newPassword");
        String confirmPassword = (String) request.get("confirmPassword");
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "两次输入的新密码不一致");
        }
        userService.changePassword(userId, oldPassword, newPassword);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/follow")
    @RequireLogin
    public ApiResponse<Map<String, Object>> followUser(@PathVariable Long id) {
        Long userId = currentUserAccessor.requireUserId();
        boolean followed = userFollowService.followUser(userId, id);
        Map<String, Object> result = new HashMap<>();
        result.put("followed", followed);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}/follow")
    @RequireLogin
    public ApiResponse<Map<String, Object>> unfollowUser(@PathVariable Long id) {
        Long userId = currentUserAccessor.requireUserId();
        boolean followed = userFollowService.unfollowUser(userId, id);
        Map<String, Object> result = new HashMap<>();
        result.put("followed", followed);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/followers")
    public ApiResponse<Map<String, Object>> getFollowers(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long currentUserId = currentUserAccessor.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("list", userFollowService.getFollowersWithUserInfo(id, currentUserId));
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/followees")
    public ApiResponse<Map<String, Object>> getFollowees(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Long currentUserId = currentUserAccessor.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("list", userFollowService.getFollowingsWithUserInfo(id, currentUserId));
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/mutual-follows")
    public ApiResponse<Map<String, Object>> getMutualFollows(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", userFollowService.getMutualFollowsWithUserInfo(id));
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/me/stats")
    @RequirePermission(anyOf = {"user:profile:read:self"})
    public ApiResponse<Map<String, Integer>> getMyStats() {
        Long userId = currentUserAccessor.requireUserId();
        Map<String, Integer> stats = userService.getUserStats(userId);
        return ApiResponse.success(stats);
    }

    @GetMapping("/me/wallet")
    @RequirePermission(anyOf = {"user:profile:read:self"})
    public ApiResponse<Map<String, Object>> getMyWalletOverview() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(walletService.getWalletOverview(userId));
    }

    @GetMapping("/me/auth-status/any")
    @RequirePermission(anyOf = {"user:profile:read:self"})
    public ApiResponse<Map<String, Object>> checkAnyAuthStatus() {
        try {
            Long userId = currentUserAccessor.requireUserId();
            return ApiResponse.success(userService.getCurrentAuthStatus(userId));
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasAnyAuth", false);
            result.put("studentVerified", false);
            result.put("merchantVerified", false);
            result.put("shouldHideAllAuth", false);
            return ApiResponse.success(result);
        }
    }
}
