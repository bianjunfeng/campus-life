package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.AiAgentEntryDTO;
import com.campus.campus_life_ai.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/ai")
public class MerchantAiController {

    @GetMapping("/agents")
    public ApiResponse<List<AiAgentEntryDTO>> getMerchantAgents() {
        return ApiResponse.success(List.of(
                new AiAgentEntryDTO(
                        "dialog",
                        "商家对话 Agent",
                        "面向商家的通用对话入口，支持快速和流式两种模式。",
                        "/merchant/ai/chat",
                        "/api/agent/conversations",
                        "chat.general"
                ),
                new AiAgentEntryDTO(
                        "knowledge",
                        "商家知识库 Agent",
                        "商家账号私有知识库，上传文档后可在对话中检索引用。",
                        "/merchant/ai/knowledge-bases",
                        "/api/agent/knowledge-bases",
                        "chat.personal_qa"
                ),
                new AiAgentEntryDTO(
                        "merchant_ops",
                        "商家运营 Agent",
                        "围绕券管理、退款审核、门店运营和平台规则提供运营建议。",
                        "/merchant/ai/chat?assistantType=merchant_ops",
                        "/api/agent/conversations/{sessionId}/messages",
                        "chat.merchant_ops"
                )
        ));
    }
}
