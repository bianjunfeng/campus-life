package com.campus.campus_life_ai.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "platform.user-service")
public class PlatformServiceProperties {

    private String baseUrl = "http://127.0.0.1:8080";
}
