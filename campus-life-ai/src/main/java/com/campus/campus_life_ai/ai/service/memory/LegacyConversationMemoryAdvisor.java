package com.campus.campus_life_ai.ai.service.memory;

import com.campus.campus_life_ai.ai.entity.AiMessage;
import com.campus.campus_life_ai.ai.mapper.AiMessageMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ai.memory.client", havingValue = "legacy")
public class LegacyConversationMemoryAdvisor implements ConversationMemoryAdvisor {

    private final AiMessageMapper aiMessageMapper;

    public LegacyConversationMemoryAdvisor(AiMessageMapper aiMessageMapper) {
        this.aiMessageMapper = aiMessageMapper;
    }

    @Override
    public List<ChatCompletionCommand.PromptMessage> buildPromptMessages(String sessionId,
                                                                         Long latestUserMessageId,
                                                                         String latestContent,
                                                                         Integer maxContextMessages) {
        List<AiMessage> context = aiMessageMapper.findRecentContext(sessionId, resolveMaxMessages(maxContextMessages));
        List<ChatCompletionCommand.PromptMessage> promptMessages = new ArrayList<>();
        for (AiMessage message : context) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            if (latestUserMessageId != null && latestUserMessageId.equals(message.getId())) {
                continue;
            }
            promptMessages.add(ChatCompletionCommand.PromptMessage.builder()
                    .role(message.getRole())
                    .content(message.getContent())
                    .build());
        }

        boolean latestIncluded = promptMessages.stream()
                .anyMatch(message -> "user".equals(message.getRole()) && latestContent.equals(message.getContent()));
        if (!latestIncluded) {
            promptMessages.add(ChatCompletionCommand.PromptMessage.builder()
                    .role("user")
                    .content(latestContent)
                    .build());
        }
        return promptMessages;
    }

    private int resolveMaxMessages(Integer maxContextMessages) {
        return maxContextMessages == null || maxContextMessages <= 0 ? 20 : maxContextMessages;
    }
}
