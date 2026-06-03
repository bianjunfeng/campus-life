package com.campus.campus_life_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    private boolean enabled = true;

    private String jwtSecret = "campus-life-secret-key-2025-please-change-in-production";

    private boolean stateCheckEnabled = true;

    private boolean failOpen = false;

    private String redisTokenPrefix = "token:";

    private String redisBlacklistPrefix = "blacklist:";

    private List<String> publicPaths = new ArrayList<>();

    private List<String> publicGetPaths = new ArrayList<>();

    private List<String> protectedPaths = new ArrayList<>();

    private List<String> protectedWritePaths = new ArrayList<>();

    private List<String> protectedPrefixes = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public boolean isStateCheckEnabled() {
        return stateCheckEnabled;
    }

    public void setStateCheckEnabled(boolean stateCheckEnabled) {
        this.stateCheckEnabled = stateCheckEnabled;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public String getRedisTokenPrefix() {
        return redisTokenPrefix;
    }

    public void setRedisTokenPrefix(String redisTokenPrefix) {
        this.redisTokenPrefix = redisTokenPrefix;
    }

    public String getRedisBlacklistPrefix() {
        return redisBlacklistPrefix;
    }

    public void setRedisBlacklistPrefix(String redisBlacklistPrefix) {
        this.redisBlacklistPrefix = redisBlacklistPrefix;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getProtectedPrefixes() {
        return protectedPrefixes;
    }

    public void setProtectedPrefixes(List<String> protectedPrefixes) {
        this.protectedPrefixes = protectedPrefixes;
    }

    public List<String> getPublicGetPaths() {
        return publicGetPaths;
    }

    public void setPublicGetPaths(List<String> publicGetPaths) {
        this.publicGetPaths = publicGetPaths;
    }

    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    public void setProtectedPaths(List<String> protectedPaths) {
        this.protectedPaths = protectedPaths;
    }

    public List<String> getProtectedWritePaths() {
        return protectedWritePaths;
    }

    public void setProtectedWritePaths(List<String> protectedWritePaths) {
        this.protectedWritePaths = protectedWritePaths;
    }
}
