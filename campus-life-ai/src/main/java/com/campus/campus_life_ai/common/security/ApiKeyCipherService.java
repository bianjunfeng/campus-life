package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.properties.AiSecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyCipherService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String VERSION_PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final AiSecurityProperties aiSecurityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCipherService(AiSecurityProperties aiSecurityProperties) {
        this.aiSecurityProperties = aiSecurityProperties;
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        if (isEncrypted(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(plainText.trim().getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("AI Provider API Key 加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        if (!isEncrypted(cipherText)) {
            return cipherText.trim();
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(VERSION_PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AI Provider API Key 解密失败", e);
        }
    }

    public boolean isEncrypted(String value) {
        return StringUtils.hasText(value) && value.startsWith(VERSION_PREFIX);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(aiSecurityProperties.getApiKeyEncryptionSecret());
    }

    private SecretKeySpec buildSecretKey() throws Exception {
        String secret = aiSecurityProperties.getApiKeyEncryptionSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("缺少 AI_API_KEY_ENCRYPTION_SECRET，无法安全读写 provider API Key");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.trim().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, SECRET_KEY_ALGORITHM);
    }
}
