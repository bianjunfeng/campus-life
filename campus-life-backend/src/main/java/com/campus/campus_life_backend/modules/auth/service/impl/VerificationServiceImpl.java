package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.SmsSender;
import com.campus.campus_life_backend.modules.auth.service.VerificationScene;
import com.campus.campus_life_backend.modules.auth.service.VerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationServiceImpl implements VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationServiceImpl.class);

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration IP_HOURLY_WINDOW = Duration.ofHours(1);
    private static final Duration FAIL_LOCK_TTL = Duration.ofMinutes(15);
    private static final int DAILY_SEND_LIMIT = 10;
    private static final int IP_HOURLY_SEND_LIMIT = 30;
    private static final int MAX_VERIFY_FAILURES = 5;

    private static final String KEY_PREFIX = "auth:verification:";

    private final RedisTemplate<String, String> redisTemplate;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String hmacSecret;
    private final boolean allowMemoryFallback;
    private final Clock clock;
    private final ConcurrentHashMap<String, MemoryValue> memoryStore = new ConcurrentHashMap<>();

    private volatile boolean useRedis;

    @Autowired
    public VerificationServiceImpl(ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider,
                                   SmsSender smsSender,
                                   Environment environment,
                                   @Value("${auth.verification-code.secret:}") String verificationCodeSecret,
                                   @Value("${jwt.secret:campus-life-secret-key-2025-please-change-in-production}") String jwtSecret) {
        this(
                redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable(),
                smsSender,
                verificationCodeSecret,
                jwtSecret,
                Set.of(environment.getActiveProfiles()),
                Clock.systemDefaultZone()
        );
    }

    public VerificationServiceImpl(RedisTemplate<String, String> redisTemplate,
                                   SmsSender smsSender,
                                   String verificationCodeSecret,
                                   String jwtSecret,
                                   Set<String> activeProfiles,
                                   Clock clock) {
        this.redisTemplate = redisTemplate;
        this.smsSender = smsSender;
        this.hmacSecret = resolveHmacSecret(verificationCodeSecret, jwtSecret);
        this.allowMemoryFallback = allowsMemoryFallback(activeProfiles);
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.useRedis = initializeRedisAvailability(redisTemplate);
    }

    @Override
    public void sendVerificationCode(String phone, String sceneCode, String clientIp) {
        String normalizedPhone = normalizePhone(phone);
        VerificationScene scene = normalizeScene(sceneCode);
        String normalizedIp = normalizeIp(clientIp);

        checkSendRateLimits(normalizedPhone, scene, normalizedIp);
        String code = generateCode();
        smsSender.sendVerificationCode(normalizedPhone, code, scene);
        saveCode(normalizedPhone, scene, code);
        logger.debug("验证码已发送 scene={}, phone={}", scene.code(), normalizedPhone);
    }

    @Override
    public boolean verifyCode(String phone, String code, String sceneCode) {
        String normalizedPhone = normalizePhone(phone);
        String normalizedCode = code == null ? "" : code.trim();
        VerificationScene scene = normalizeScene(sceneCode);

        if (isLocked(normalizedPhone, scene)) {
            throw new BusinessException(
                    BusinessErrorCode.TOO_MANY_REQUESTS,
                    "验证码错误次数过多，请15分钟后重试"
            );
        }

        String storedHash = getValue(codeKey(scene, normalizedPhone));
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String submittedHash = hmac(scene, normalizedPhone, normalizedCode);
        if (constantTimeEquals(storedHash, submittedHash)) {
            deleteValue(codeKey(scene, normalizedPhone));
            deleteValue(failKey(scene, normalizedPhone));
            return true;
        }

        long failCount = increment(failKey(scene, normalizedPhone));
        if (failCount >= MAX_VERIFY_FAILURES) {
            expire(failKey(scene, normalizedPhone), FAIL_LOCK_TTL);
            throw new BusinessException(
                    BusinessErrorCode.TOO_MANY_REQUESTS,
                    "验证码错误次数过多，请15分钟后重试"
            );
        }
        expire(failKey(scene, normalizedPhone), CODE_TTL);
        return false;
    }

    private boolean initializeRedisAvailability(RedisTemplate<String, String> template) {
        if (template == null) {
            if (allowMemoryFallback) {
                logger.info("Redis 未配置，使用内存存储验证码（仅用于开发测试）");
                return false;
            }
            logger.warn("Redis 未配置，验证码服务将不可用");
            return false;
        }
        try {
            template.opsForValue().set(KEY_PREFIX + "connection:test", "ok", 1, TimeUnit.SECONDS);
            template.delete(KEY_PREFIX + "connection:test");
            logger.info("Redis 连接成功，使用Redis存储验证码");
            return true;
        } catch (Exception e) {
            if (allowMemoryFallback) {
                logger.warn("Redis 连接失败，使用内存存储验证码（仅用于开发测试）: {}", e.getMessage());
                return false;
            }
            logger.warn("Redis 连接失败，验证码服务将不可用: {}", e.getMessage());
            return false;
        }
    }

    private void checkSendRateLimits(String phone, VerificationScene scene, String clientIp) {
        long ipCount = increment(ipHourlyKey(clientIp));
        if (ipCount == 1) {
            expire(ipHourlyKey(clientIp), IP_HOURLY_WINDOW);
        }
        if (ipCount > IP_HOURLY_SEND_LIMIT) {
            throw new BusinessException(
                    BusinessErrorCode.TOO_MANY_REQUESTS,
                    "当前IP发送验证码过于频繁，请稍后重试"
            );
        }

        long dailyCount = increment(dailyKey(scene, phone));
        if (dailyCount == 1) {
            expire(dailyKey(scene, phone), durationUntilNextDay());
        }
        if (dailyCount > DAILY_SEND_LIMIT) {
            throw new BusinessException(
                    BusinessErrorCode.TOO_MANY_REQUESTS,
                    "该手机号今日验证码发送次数已达上限"
            );
        }

        boolean cooldownAcquired = setIfAbsent(cooldownKey(scene, phone), "1", SEND_COOLDOWN);
        if (!cooldownAcquired) {
            throw new BusinessException(
                    BusinessErrorCode.TOO_MANY_REQUESTS,
                    "验证码发送过于频繁，请60秒后再试"
            );
        }
    }

    private void saveCode(String phone, VerificationScene scene, String code) {
        setValue(codeKey(scene, phone), hmac(scene, phone, code), CODE_TTL);
    }

    private boolean isLocked(String phone, VerificationScene scene) {
        String failCount = getValue(failKey(scene, phone));
        if (failCount == null || failCount.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(failCount) >= MAX_VERIFY_FAILURES;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void setValue(String key, String value, Duration ttl) {
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }
        requireMemoryFallbackAllowed();
        memorySet(key, value, ttl);
    }

    private String getValue(String key) {
        if (useRedis && redisTemplate != null) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }
        requireMemoryFallbackAllowed();
        return memoryGet(key);
    }

    private void deleteValue(String key) {
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }
        requireMemoryFallbackAllowed();
        memoryDelete(key);
    }

    private long increment(String key) {
        if (useRedis && redisTemplate != null) {
            try {
                Long value = redisTemplate.opsForValue().increment(key);
                return value == null ? 0L : value;
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }
        requireMemoryFallbackAllowed();
        return memoryIncrement(key);
    }

    private boolean setIfAbsent(String key, String value, Duration ttl) {
        if (useRedis && redisTemplate != null) {
            try {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
                return Boolean.TRUE.equals(success);
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }
        requireMemoryFallbackAllowed();
        return memorySetIfAbsent(key, value, ttl);
    }

    private void expire(String key, Duration ttl) {
        if (useRedis && redisTemplate != null) {
            try {
                redisTemplate.expire(key, ttl.toSeconds(), TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }
        requireMemoryFallbackAllowed();
        memoryExpire(key, ttl);
    }

    private void handleRedisFailure(Exception e) {
        if (allowMemoryFallback) {
            useRedis = false;
            logger.warn("Redis 操作失败，降级到内存存储（仅用于开发测试）: {}", e.getMessage());
            return;
        }
        throw new BusinessException(
                BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE,
                "验证码服务暂不可用，请稍后重试",
                e
        );
    }

    private void requireMemoryFallbackAllowed() {
        if (!allowMemoryFallback) {
            throw new BusinessException(
                    BusinessErrorCode.AUTH_SERVICE_UNAVAILABLE,
                    "验证码服务暂不可用，请稍后重试"
            );
        }
    }

    private synchronized void memorySet(String key, String value, Duration ttl) {
        cleanupExpiredMemoryValues();
        memoryStore.put(key, new MemoryValue(value, expiresAt(ttl)));
    }

    private synchronized String memoryGet(String key) {
        MemoryValue value = memoryStore.get(key);
        if (value == null) {
            return null;
        }
        if (value.isExpired(clock)) {
            memoryStore.remove(key);
            return null;
        }
        return value.value;
    }

    private synchronized void memoryDelete(String key) {
        memoryStore.remove(key);
    }

    private synchronized long memoryIncrement(String key) {
        cleanupExpiredMemoryValues();
        MemoryValue current = memoryStore.get(key);
        long next = 1L;
        long expireAtMillis = 0L;
        if (current != null && !current.isExpired(clock)) {
            try {
                next = Long.parseLong(current.value) + 1L;
            } catch (NumberFormatException ignored) {
                next = 1L;
            }
            expireAtMillis = current.expireAtMillis;
        }
        memoryStore.put(key, new MemoryValue(String.valueOf(next), expireAtMillis));
        return next;
    }

    private synchronized boolean memorySetIfAbsent(String key, String value, Duration ttl) {
        cleanupExpiredMemoryValues();
        MemoryValue current = memoryStore.get(key);
        if (current != null && !current.isExpired(clock)) {
            return false;
        }
        memoryStore.put(key, new MemoryValue(value, expiresAt(ttl)));
        return true;
    }

    private synchronized void memoryExpire(String key, Duration ttl) {
        MemoryValue current = memoryStore.get(key);
        if (current != null && !current.isExpired(clock)) {
            memoryStore.put(key, new MemoryValue(current.value, expiresAt(ttl)));
        }
    }

    private synchronized void cleanupExpiredMemoryValues() {
        memoryStore.entrySet().removeIf(entry -> entry.getValue().isExpired(clock));
    }

    private long expiresAt(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return 0L;
        }
        return clock.millis() + ttl.toMillis();
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String hmac(VerificationScene scene, String phone, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((scene.code() + ":" + phone + ":" + code).getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "验证码安全组件初始化失败", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private VerificationScene normalizeScene(String sceneCode) {
        try {
            return VerificationScene.fromCode(sceneCode);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, e.getMessage(), e);
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        String firstIp = clientIp.split(",")[0].trim();
        return firstIp.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String codeKey(VerificationScene scene, String phone) {
        return KEY_PREFIX + "code:" + scene.code() + ":" + phone;
    }

    private String cooldownKey(VerificationScene scene, String phone) {
        return KEY_PREFIX + "cooldown:" + scene.code() + ":" + phone;
    }

    private String dailyKey(VerificationScene scene, String phone) {
        return KEY_PREFIX + "daily:" + LocalDate.now(clock) + ":" + scene.code() + ":" + phone;
    }

    private String ipHourlyKey(String ip) {
        return KEY_PREFIX + "ip_hourly:" + ip;
    }

    private String failKey(VerificationScene scene, String phone) {
        return KEY_PREFIX + "fail:" + scene.code() + ":" + phone;
    }

    private Duration durationUntilNextDay() {
        ZoneId zone = clock.getZone();
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime nextDay = now.toLocalDate().plusDays(1).atStartOfDay(zone);
        return Duration.between(now, nextDay);
    }

    private String resolveHmacSecret(String verificationCodeSecret, String jwtSecret) {
        if (verificationCodeSecret != null && !verificationCodeSecret.isBlank()) {
            return verificationCodeSecret;
        }
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            return jwtSecret;
        }
        throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "验证码HMAC密钥未配置");
    }

    private boolean allowsMemoryFallback(Set<String> activeProfiles) {
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return false;
        }
        return activeProfiles.stream()
                .anyMatch(profile -> Arrays.asList("local", "dev", "test").contains(profile));
    }

    private static class MemoryValue {
        private final String value;
        private final long expireAtMillis;

        private MemoryValue(String value, long expireAtMillis) {
            this.value = value;
            this.expireAtMillis = expireAtMillis;
        }

        private boolean isExpired(Clock clock) {
            return expireAtMillis > 0 && clock.millis() >= expireAtMillis;
        }
    }
}
