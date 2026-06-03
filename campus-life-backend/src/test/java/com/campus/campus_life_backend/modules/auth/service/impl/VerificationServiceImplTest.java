package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.SmsSender;
import com.campus.campus_life_backend.modules.auth.service.VerificationScene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationServiceImplTest {

    private CapturingSmsSender smsSender;
    private MutableClock clock;
    private VerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        smsSender = new CapturingSmsSender();
        clock = new MutableClock(Instant.parse("2026-06-02T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        verificationService = new VerificationServiceImpl(
                null,
                smsSender,
                "verification-secret",
                "jwt-secret",
                Set.of("test"),
                clock
        );
    }

    @Test
    void differentScenesCannotShareCode() {
        verificationService.sendVerificationCode("13800138000", "register", "127.0.0.1");
        String registerCode = smsSender.lastCode();

        assertFalse(verificationService.verifyCode("13800138000", registerCode, "login"));
        assertTrue(verificationService.verifyCode("13800138000", registerCode, "register"));
    }

    @Test
    void samePhoneAndSceneShouldRespectSixtySecondCooldown() {
        verificationService.sendVerificationCode("13800138000", "login", "127.0.0.1");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> verificationService.sendVerificationCode("13800138000", "login", "127.0.0.1"));

        assertEquals(BusinessErrorCode.TOO_MANY_REQUESTS.getCode(), exception.getCode());
        assertEquals("验证码发送过于频繁，请60秒后再试", exception.getMessage());
    }

    @Test
    void samePhoneAndSceneShouldRespectDailyLimit() {
        for (int i = 0; i < 10; i++) {
            verificationService.sendVerificationCode("13800138000", "register", "127.0.0.1");
            clock.advance(Duration.ofSeconds(60));
        }

        BusinessException exception = assertThrows(BusinessException.class,
                () -> verificationService.sendVerificationCode("13800138000", "register", "127.0.0.1"));

        assertEquals(BusinessErrorCode.TOO_MANY_REQUESTS.getCode(), exception.getCode());
        assertEquals("该手机号今日验证码发送次数已达上限", exception.getMessage());
    }

    @Test
    void sameIpShouldRespectHourlyLimit() {
        for (int i = 0; i < 30; i++) {
            verificationService.sendVerificationCode(String.format("13800138%03d", i), "login", "10.0.0.1");
        }

        BusinessException exception = assertThrows(BusinessException.class,
                () -> verificationService.sendVerificationCode("13800138999", "login", "10.0.0.1"));

        assertEquals(BusinessErrorCode.TOO_MANY_REQUESTS.getCode(), exception.getCode());
        assertEquals("当前IP发送验证码过于频繁，请稍后重试", exception.getMessage());
    }

    @Test
    void fiveWrongAttemptsShouldLockVerificationForFifteenMinutes() {
        verificationService.sendVerificationCode("13800138000", "login", "127.0.0.1");

        for (int i = 0; i < 4; i++) {
            assertFalse(verificationService.verifyCode("13800138000", "000000", "login"));
        }

        BusinessException lockException = assertThrows(BusinessException.class,
                () -> verificationService.verifyCode("13800138000", "000000", "login"));
        assertEquals(BusinessErrorCode.TOO_MANY_REQUESTS.getCode(), lockException.getCode());

        BusinessException stillLocked = assertThrows(BusinessException.class,
                () -> verificationService.verifyCode("13800138000", smsSender.lastCode(), "login"));
        assertEquals(BusinessErrorCode.TOO_MANY_REQUESTS.getCode(), stillLocked.getCode());
    }

    @Test
    void successfulVerificationShouldConsumeCodeAndClearFailureCount() {
        verificationService.sendVerificationCode("13800138000", "login", "127.0.0.1");
        String firstCode = smsSender.lastCode();
        assertFalse(verificationService.verifyCode("13800138000", "000000", "login"));

        assertTrue(verificationService.verifyCode("13800138000", firstCode, "login"));
        assertFalse(verificationService.verifyCode("13800138000", firstCode, "login"));

        clock.advance(Duration.ofSeconds(60));
        verificationService.sendVerificationCode("13800138000", "login", "127.0.0.1");
        for (int i = 0; i < 4; i++) {
            assertFalse(verificationService.verifyCode("13800138000", "000000", "login"));
        }
    }

    private static class CapturingSmsSender implements SmsSender {
        private final List<String> codes = new ArrayList<>();

        @Override
        public void sendVerificationCode(String phone, String code, VerificationScene scene) {
            codes.add(code);
        }

        private String lastCode() {
            return codes.get(codes.size() - 1);
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
