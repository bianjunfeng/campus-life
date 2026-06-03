package com.campus.campus_life_ai.ai.service.memory;

import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;

import java.util.List;

public interface ConversationMemoryAdvisor {

    List<ChatCompletionCommand.PromptMessage> buildPromptMessages(String sessionId,
                                                                  Long latestUserMessageId,
                                                                  String latestContent,
                                                                  Integer maxContextMessages);
}
