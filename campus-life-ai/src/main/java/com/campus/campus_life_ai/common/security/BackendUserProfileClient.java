package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.properties.PlatformServiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BackendUserProfileClient {

    private final RestClient restClient;
    private final PlatformServiceProperties platformServiceProperties;

    public BackendUserProfileClient(RestClient.Builder restClientBuilder,
                                    PlatformServiceProperties platformServiceProperties) {
        this.restClient = restClientBuilder.build();
        this.platformServiceProperties = platformServiceProperties;
    }

    public String fetchCurrentUserRole(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }
        try {
            JsonNode response = restClient.get()
                    .uri(platformServiceProperties.getBaseUrl() + "/api/users/me")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(JsonNode.class);
            String role = response == null ? null : response.path("data").path("role").asText(null);
            return StringUtils.hasText(role) ? role.trim() : null;
        } catch (Exception e) {
            log.warn("回源校验用户角色失败, url={}", platformServiceProperties.getBaseUrl(), e);
            return null;
        }
    }
}
