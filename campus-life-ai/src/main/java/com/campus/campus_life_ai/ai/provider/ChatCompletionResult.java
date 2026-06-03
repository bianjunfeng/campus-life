package com.campus.campus_life_ai.ai.provider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatCompletionResult {
    String content;
    String providerCode;
    String modelCode;
    Integer promptTokens;
    Integer completionTokens;
    Integer totalTokens;
    Integer latencyMs;
    String finishReason;
}
