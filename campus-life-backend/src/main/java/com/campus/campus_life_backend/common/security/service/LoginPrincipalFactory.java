package com.campus.campus_life_backend.common.security.service;

import com.campus.campus_life_backend.common.security.model.LoginPrincipal;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class LoginPrincipalFactory {

    private final UserMapper userMapper;
    private final RolePermissionRegistry rolePermissionRegistry;

    public LoginPrincipalFactory(UserMapper userMapper, RolePermissionRegistry rolePermissionRegistry) {
        this.userMapper = userMapper;
        this.rolePermissionRegistry = rolePermissionRegistry;
    }

    @Nullable
    public LoginPrincipal create(Long userId) {
        if (userId == null) {
            return null;
        }

        User user = userMapper.findById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }

        RoleCode roleCode = RoleCode.fromDbRole(user.getRole());
        return new LoginPrincipal(userId, roleCode, rolePermissionRegistry.getPermissions(roleCode));
    }
}
