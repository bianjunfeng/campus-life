package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.AgentConversationSummaryDTO;
import com.campus.campus_life_ai.ai.dto.AgentMessageDTO;
import com.campus.campus_life_ai.ai.dto.SendMessageResponse;
import com.campus.campus_life_ai.ai.dto.UsageDTO;
import com.campus.campus_life_ai.ai.service.AgentChatService;
import com.campus.campus_life_ai.ai.service.AgentConversationService;
import com.campus.campus_life_ai.common.config.GlobalExceptionHandler;
import com.campus.campus_life_ai.common.config.MerchantAiAuthorizationInterceptor;
import com.campus.campus_life_ai.common.exception.BusinessErrorCode;
import com.campus.campus_life_ai.common.security.AdminAuthorizationService;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AgentConversationController.class)
@Import(GlobalExceptionHandler.class)
class AgentConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    @MockitoBean
    private AdminAuthorizationService adminAuthorizationService;

    @MockitoBean
    private MerchantAiAuthorizationInterceptor merchantAiAuthorizationInterceptor;

    @MockitoBean
    private AgentConversationService agentConversationService;

    @MockitoBean
    private AgentChatService agentChatService;

    @Test
    void shouldListConversations() throws Exception {
        AgentConversationSummaryDTO summary = new AgentConversationSummaryDTO();
        summary.setSessionId("ses_123");
        summary.setTitle("测试会话");
        summary.setAssistantType("general");
        summary.setMessageCount(2);
        summary.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 0));

        given(currentUserAccessor.requireUserId()).willReturn(1001L);
        given(agentConversationService.listConversations(1001L)).willReturn(List.of(summary));

        mockMvc.perform(get("/api/agent/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].sessionId").value("ses_123"))
                .andExpect(jsonPath("$.data[0].title").value("测试会话"));
    }

    @Test
    void shouldSendMessage() throws Exception {
        AgentMessageDTO userMessage = new AgentMessageDTO();
        userMessage.setSessionId("ses_123");
        userMessage.setRole("user");
        userMessage.setContent("你好");
        userMessage.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 1));

        AgentMessageDTO assistantMessage = new AgentMessageDTO();
        assistantMessage.setSessionId("ses_123");
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("你好，我是校园生活助手");
        assistantMessage.setMessageStatus("SUCCESS");
        assistantMessage.setCreatedAt(LocalDateTime.of(2026, 4, 20, 10, 1, 1));

        UsageDTO usageDTO = new UsageDTO();
        usageDTO.setPromptTokens(12);
        usageDTO.setCompletionTokens(18);
        usageDTO.setTotalTokens(30);
        usageDTO.setLatencyMs(520);

        SendMessageResponse response = new SendMessageResponse();
        response.setSessionId("ses_123");
        response.setConversationTitle("你好");
        response.setUserMessage(userMessage);
        response.setAssistantMessage(assistantMessage);
        response.setUsage(usageDTO);

        given(currentUserAccessor.requireUserId()).willReturn(1001L);
        given(agentChatService.sendMessage(eq(1001L), eq("ses_123"), any())).willReturn(response);

        mockMvc.perform(post("/api/agent/conversations/ses_123/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "你好"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("ses_123"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("你好，我是校园生活助手"))
                .andExpect(jsonPath("$.data.usage.totalTokens").value(30));
    }

    @Test
    void shouldRejectBlankMessage() throws Exception {
        given(currentUserAccessor.requireUserId()).willReturn(1001L);

        mockMvc.perform(post("/api/agent/conversations/ses_123/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": ""
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BusinessErrorCode.INVALID_PARAM.getCode()))
                .andExpect(jsonPath("$.message").value("消息内容不能为空"));
    }
}
