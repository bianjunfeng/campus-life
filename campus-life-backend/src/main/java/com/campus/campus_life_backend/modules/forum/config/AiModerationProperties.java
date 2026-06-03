package com.campus.campus_life_backend.modules.forum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.moderation")
public class AiModerationProperties {

    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:8083";
    private boolean failOpen = true;
    private boolean blockReview = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public boolean isBlockReview() {
        return blockReview;
    }

    public void setBlockReview(boolean blockReview) {
        this.blockReview = blockReview;
    }
}
