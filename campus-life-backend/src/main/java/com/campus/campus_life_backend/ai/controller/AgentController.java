package com.campus.campus_life_backend.ai.controller;

import com.campus.campus_life_backend.ai.dto.ChatRequest;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequirePermission(anyOf = {"ai:use"})
@Deprecated(since = "phase-3")
public class AgentController {

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Void>> chat(@RequestBody(required = false) ChatRequest request) {
        throw legacyAiMoved();
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Void>> getChatHistory() {
        throw legacyAiMoved();
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearChatHistory() {
        throw legacyAiMoved();
    }

    private BusinessException legacyAiMoved() {
        return new BusinessException(
                BusinessErrorCode.LEGACY_API_GONE,
                "旧 AI 接口已迁移，请通过网关访问 campus-life-ai 的 /api/agent/** 接口"
        );
    }
}
