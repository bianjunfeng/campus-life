package com.campus.campus_life_ai.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.security")
public class AiSecurityProperties {

    private String apiKeyEncryptionSecret;
}
