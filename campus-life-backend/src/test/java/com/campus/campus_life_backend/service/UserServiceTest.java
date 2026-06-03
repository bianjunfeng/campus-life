package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.auth.service.TokenService;
import com.campus.campus_life_backend.modules.auth.service.AuthSessionService;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.user.entity.User;
import com.campus.campus_life_backend.modules.user.mapper.StudentProfileMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserFollowMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import com.campus.campus_life_backend.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private StudentProfileMapper studentProfileMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userMapper,
                postMapper,
                userFollowMapper,
                studentProfileMapper,
                merchantMapper,
                passwordEncoder,
                tokenService,
                null,
                null
        );
    }

    @Test
    void changePasswordShouldRevokeAllUserTokensAfterPasswordUpdated() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("$2a$10$mockedBcryptHashForTestingOnly123456789012");

        given(userMapper.findById(1L)).willReturn(user);
        given(passwordEncoder.matches("oldPass123", user.getPasswordHash())).willReturn(true);
        given(passwordEncoder.encode("newPass123")).willReturn("encoded-new-password");

        userService.changePassword(1L, "oldPass123", "newPass123");

        InOrder inOrder = inOrder(userMapper, tokenService);
        inOrder.verify(userMapper).updatePassword(1L, "encoded-new-password");
        inOrder.verify(tokenService).revokeAllUserTokens(1L, AuthSessionService.STATUS_LOGGED_OUT);
    }

    @Test
    void adminUpdateStatusShouldRevokeAllUserTokensWhenDisablingAccount() {
        User user = new User();
        user.setId(2L);
        user.setStatus(1);

        given(userMapper.findById(2L)).willReturn(user);

        UserService.AdminUserStatusChange change = userService.adminUpdateStatus(2L, 0);

        assertEquals(1, change.oldStatus());
        assertEquals(0, change.newStatus());
        verify(userMapper).updateStatus(2L, 0);
        verify(tokenService).revokeAllUserTokens(2L, AuthSessionService.STATUS_LOGGED_OUT);
    }
}
