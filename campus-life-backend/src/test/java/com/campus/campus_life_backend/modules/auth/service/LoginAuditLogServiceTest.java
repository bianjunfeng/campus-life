package com.campus.campus_life_backend.modules.auth.service;

import com.campus.campus_life_backend.modules.auth.mapper.LoginAuditLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoginAuditLogServiceTest {

    @Mock
    private LoginAuditLogMapper loginAuditLogMapper;

    @InjectMocks
    private LoginAuditLogService loginAuditLogService;

    @Test
    void getMemberLoginLogsShouldExposeFrontendAliases() {
        LocalDateTime createTime = LocalDateTime.of(2026, 6, 4, 10, 15);
        given(loginAuditLogMapper.findMemberLoginLogs(0, 10)).willReturn(List.of(Map.of(
                "id", 1L,
                "userId", 7L,
                "username", "student",
                "ip", "127.0.0.1",
                "createTime", createTime
        )));
        given(loginAuditLogMapper.countMemberLoginLogs()).willReturn(1L);

        Map<String, Object> page = loginAuditLogService.getMemberLoginLogs(1, 10);
        Map<String, Object> row = firstRow(page);

        assertEquals(7L, row.get("memberId"));
        assertEquals("127.0.0.1", row.get("loginIp"));
        assertEquals("本机", row.get("loginLocation"));
        assertEquals(createTime, row.get("loginTime"));
    }

    @Test
    void getAdminLoginLogsShouldExposeFrontendAliases() {
        LocalDateTime createTime = LocalDateTime.of(2026, 6, 4, 11, 20);
        given(loginAuditLogMapper.findAdminLoginLogs(0, 10)).willReturn(List.of(Map.of(
                "id", 2L,
                "userId", 99L,
                "username", "admin",
                "ip", "192.168.1.20",
                "createTime", createTime
        )));
        given(loginAuditLogMapper.countAdminLoginLogs()).willReturn(1L);

        Map<String, Object> page = loginAuditLogService.getAdminLoginLogs(1, 10);
        Map<String, Object> row = firstRow(page);

        assertEquals(99L, row.get("adminId"));
        assertEquals("192.168.1.20", row.get("loginIp"));
        assertEquals("内网", row.get("loginLocation"));
        assertEquals(createTime, row.get("loginTime"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstRow(Map<String, Object> page) {
        return ((List<Map<String, Object>>) page.get("list")).get(0);
    }
}
