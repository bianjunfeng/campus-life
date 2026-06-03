package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.merchant.mapper.MerchantAuthRequestMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.merchant.service.MerchantAuthService;
import com.campus.campus_life_backend.modules.user.mapper.StudentAuthRequestMapper;
import com.campus.campus_life_backend.modules.user.mapper.StudentProfileMapper;
import com.campus.campus_life_backend.modules.user.service.AuthRequestService;
import com.campus.campus_life_backend.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRequestBoundaryServiceTest {

    @Mock
    private StudentAuthRequestMapper studentAuthRequestMapper;

    @Mock
    private MerchantAuthRequestMapper merchantAuthRequestMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private StudentProfileMapper studentProfileMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserService userService;

    private AuthRequestService authRequestService;
    private MerchantAuthService merchantAuthService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS)))
                .thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn(null);

        authRequestService = new AuthRequestService(studentAuthRequestMapper, studentProfileMapper, stringRedisTemplate, userService);
        merchantAuthService = new MerchantAuthService(merchantAuthRequestMapper, merchantMapper, stringRedisTemplate, userService);
    }

    @Test
    void shouldRejectStudentAuthSubmissionWhenAnyAuthAlreadyVerified() {
        when(userService.hasAnyAuthVerified(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authRequestService.submitStudentAuthRequest(1L, Map.of(
                        "realName", "测试用户",
                        "school", "测试大学",
                        "studentNo", "20250001",
                        "studentCardImg", "/uploads/test.png"
                )));

        assertEquals("您已通过其他认证，无需重复提交", exception.getMessage());
        verify(studentAuthRequestMapper, never()).findByUserId(anyLong());
        verify(studentAuthRequestMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectMerchantAuthSubmissionWhenAnyAuthAlreadyVerified() {
        when(userService.hasAnyAuthVerified(2L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                merchantAuthService.submitMerchantAuthRequest(2L, Map.of(
                        "merchantName", "测试商家",
                        "licenseImg", "/uploads/license.png"
                )));

        assertEquals("您已通过其他认证，无需重复提交", exception.getMessage());
        verify(merchantAuthRequestMapper, never()).findByUserId(anyLong());
        verify(merchantAuthRequestMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }
}
