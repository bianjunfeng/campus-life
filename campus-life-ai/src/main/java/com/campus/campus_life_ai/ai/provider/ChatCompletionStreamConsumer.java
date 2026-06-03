package com.campus.campus_life_ai.ai.provider;

@FunctionalInterface
public interface ChatCompletionStreamConsumer {

    void onDelta(String content);
}
