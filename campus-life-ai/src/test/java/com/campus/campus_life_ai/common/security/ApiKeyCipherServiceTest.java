package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.properties.AiSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyCipherServiceTest {

    @Test
    void shouldEncryptAndDecryptApiKey() {
        AiSecurityProperties properties = new AiSecurityProperties();
        properties.setApiKeyEncryptionSecret("unit-test-secret");
        ApiKeyCipherService service = new ApiKeyCipherService(properties);

        String encrypted = service.encrypt("sk-test");

        assertTrue(service.isEncrypted(encrypted));
        assertNotEquals("sk-test", encrypted);
        assertEquals("sk-test", service.decrypt(encrypted));
    }

    @Test
    void shouldKeepLegacyPlaintextReadable() {
        AiSecurityProperties properties = new AiSecurityProperties();
        properties.setApiKeyEncryptionSecret("unit-test-secret");
        ApiKeyCipherService service = new ApiKeyCipherService(properties);

        assertEquals("legacy-key", service.decrypt("legacy-key"));
    }
}
