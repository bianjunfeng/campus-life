package com.campus.campus_life_ai.ai.service.memory;

import com.campus.campus_life_ai.ai.entity.AiMessage;
import com.campus.campus_life_ai.ai.mapper.AiMessageMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "ai.memory.client", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiConversationMemoryAdvisor implements ConversationMemoryAdvisor {

    private final AiMessageMapper aiMessageMapper;

    public SpringAiConversationMemoryAdvisor(AiMessageMapper aiMessageMapper) {
        this.aiMessageMapper = aiMessageMapper;
    }

    @Override
    public List<ChatCompletionCommand.PromptMessage> buildPromptMessages(String sessionId,
                                                                         Long latestUserMessageId,
                                                                         String latestContent,
                                                                         Integer maxContextMessages) {
        int maxMessages = resolveMaxMessages(maxContextMessages);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(maxMessages)
                .build();

        List<Message> historyMessages = loadHistoryMessages(sessionId, latestUserMessageId, maxMessages);
        if (!historyMessages.isEmpty()) {
            chatMemory.add(sessionId, historyMessages);
        }
        if (StringUtils.hasText(latestContent)) {
            chatMemory.add(sessionId, new UserMessage(latestContent));
        }
        return chatMemory.get(sessionId).stream()
                .map(this::toPromptMessage)
                .filter(message -> message != null && StringUtils.hasText(message.getContent()))
                .toList();
    }

    private List<Message> loadHistoryMessages(String sessionId, Long latestUserMessageId, int maxMessages) {
        List<AiMessage> context = aiMessageMapper.findRecentContext(sessionId, maxMessages);
        List<Message> messages = new ArrayList<>();
        for (AiMessage message : context) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            if (latestUserMessageId != null && latestUserMessageId.equals(message.getId())) {
                continue;
            }
            Message springMessage = toSpringMessage(message);
            if (springMessage != null) {
                messages.add(springMessage);
            }
        }
        return messages;
    }

    private Message toSpringMessage(AiMessage message) {
        String role = message.getRole() == null ? "user" : message.getRole().trim().toLowerCase(Locale.ROOT);
        return switch (role) {
            case "assistant" -> new AssistantMessage(message.getContent());
            case "system" -> new SystemMessage(message.getContent());
            case "user" -> new UserMessage(message.getContent());
            default -> null;
        };
    }

    private ChatCompletionCommand.PromptMessage toPromptMessage(Message message) {
        MessageType messageType = message.getMessageType();
        String role = messageType == null ? "user" : messageType.getValue();
        return ChatCompletionCommand.PromptMessage.builder()
                .role(role)
                .content(message.getText())
                .build();
    }

    private int resolveMaxMessages(Integer maxContextMessages) {
        return maxContextMessages == null || maxContextMessages <= 0 ? 20 : maxContextMessages;
    }
}
