package com.campus.campus_life_ai.ai.provider;

public interface LlmProvider {

    String providerCode();

    boolean isAvailable(ProviderRuntimeConfig runtimeConfig);

    ChatCompletionResult chat(ChatCompletionCommand command, ProviderRuntimeConfig runtimeConfig);

    default ChatCompletionResult stream(ChatCompletionCommand command,
                                        ProviderRuntimeConfig runtimeConfig,
                                        ChatCompletionStreamConsumer consumer) {
        ChatCompletionResult result = chat(command, runtimeConfig);
        if (consumer != null && result != null && result.getContent() != null && !result.getContent().isBlank()) {
            consumer.onDelta(result.getContent());
        }
        return result;
    }
}
