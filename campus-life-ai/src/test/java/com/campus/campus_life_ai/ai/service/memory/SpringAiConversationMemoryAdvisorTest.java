package com.campus.campus_life_ai.ai.service.memory;

import com.campus.campus_life_ai.ai.entity.AiMessage;
import com.campus.campus_life_ai.ai.mapper.AiMessageMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SpringAiConversationMemoryAdvisorTest {

    @Test
    void shouldBuildWindowedPromptMessagesWithLatestUserContent() {
        AiMessageMapper aiMessageMapper = mock(AiMessageMapper.class);
        SpringAiConversationMemoryAdvisor advisor = new SpringAiConversationMemoryAdvisor(aiMessageMapper);
        given(aiMessageMapper.findRecentContext("ses_memory", 3)).willReturn(List.of(
                message(1L, "user", "第一轮问题"),
                message(2L, "assistant", "第一轮回答"),
                message(3L, "user", "原始问题")
        ));

        List<ChatCompletionCommand.PromptMessage> messages = advisor.buildPromptMessages(
                "ses_memory",
                3L,
                "带知识库上下文的问题",
                3
        );

        assertEquals(3, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("第一轮问题", messages.get(0).getContent());
        assertEquals("assistant", messages.get(1).getRole());
        assertEquals("第一轮回答", messages.get(1).getContent());
        assertEquals("user", messages.get(2).getRole());
        assertEquals("带知识库上下文的问题", messages.get(2).getContent());
    }

    @Test
    void shouldTrimOldMessagesBySpringAiWindow() {
        AiMessageMapper aiMessageMapper = mock(AiMessageMapper.class);
        SpringAiConversationMemoryAdvisor advisor = new SpringAiConversationMemoryAdvisor(aiMessageMapper);
        given(aiMessageMapper.findRecentContext("ses_memory", 2)).willReturn(List.of(
                message(10L, "assistant", "较早回答"),
                message(11L, "user", "原始问题")
        ));

        List<ChatCompletionCommand.PromptMessage> messages = advisor.buildPromptMessages(
                "ses_memory",
                11L,
                "最新问题",
                2
        );

        assertEquals(2, messages.size());
        assertEquals("assistant", messages.get(0).getRole());
        assertEquals("较早回答", messages.get(0).getContent());
        assertEquals("user", messages.get(1).getRole());
        assertEquals("最新问题", messages.get(1).getContent());
    }

    private AiMessage message(Long id, String role, String content) {
        AiMessage message = new AiMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
