package com.campus.campus_life_ai.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProviderProperties {

    private String defaultProviderCode = "openai-compatible";
    private String defaultModelCode = "deepseek-chat";
    private String defaultAssistantType = "general";
    private String systemPrompt = "你是校园生活平台的 AI 助手，回答要准确、简洁，优先基于平台已知信息，不要编造不存在的平台规则。";
    private int maxContextMessages = 20;
    private double temperature = 0.7D;
    private int maxOutputTokens = 1024;
    private GatewayProperties gateway = new GatewayProperties();
    private ToolProperties tools = new ToolProperties();
    private OpenAiCompatibleProperties openaiCompatible = new OpenAiCompatibleProperties();

    @Data
    public static class GatewayProperties {
        private boolean enabled = true;
        private int maxInputChars = 32000;
        private int healthFailureThreshold = 2;
        private long healthCooldownMs = 30000L;
    }

    @Data
    public static class ToolProperties {
        private boolean enabled = true;
        private boolean opsEnabled = true;
    }

    @Data
    public static class OpenAiCompatibleProperties {
        private boolean enabled = false;
        private String providerCode = "openai-compatible";
        private String providerName = "OpenAI Compatible";
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "gpt-4o-mini";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
    }
}
