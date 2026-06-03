package com.campus.campus_life_ai.ai.provider;

import lombok.Builder;
import lombok.Value;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ChatCompletionCommand {
    String providerCode;
    String modelCode;
    String systemPrompt;
    Double temperature;
    Integer maxOutputTokens;
    List<PromptMessage> messages;
    List<ToolCallback> toolCallbacks;
    Map<String, Object> toolContext;

    @Value
    @Builder
    public static class PromptMessage {
        String role;
        String content;
    }
}
