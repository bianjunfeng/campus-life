package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.AiInvokeRequest;
import com.campus.campus_life_ai.ai.dto.AiModerationCheckRequest;
import com.campus.campus_life_ai.ai.dto.AiModerationCheckResponse;
import com.campus.campus_life_ai.ai.service.ModerationGatewayService;
import com.campus.campus_life_ai.common.exception.BusinessErrorCode;
import com.campus.campus_life_ai.common.exception.BusinessException;
import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiCapabilityController {

    private final CurrentUserAccessor currentUserAccessor;
    private final ModerationGatewayService moderationGatewayService;

    public AiCapabilityController(CurrentUserAccessor currentUserAccessor,
                                  ModerationGatewayService moderationGatewayService) {
        this.currentUserAccessor = currentUserAccessor;
        this.moderationGatewayService = moderationGatewayService;
    }

    @PostMapping("/generation/tasks")
    public ApiResponse<Map<String, Object>> createGenerationTask(@RequestBody(required = false) AiInvokeRequest request) {
        currentUserAccessor.requireUserId();
        throw new BusinessException(BusinessErrorCode.NOT_IMPLEMENTED, "AI 内容生成能力尚未启用，接口已预留");
    }

    @GetMapping("/generation/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> getGenerationTask(@PathVariable String taskId) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(Map.of("taskId", taskId, "status", "RESERVED"));
    }

    @PostMapping("/recommendations/query")
    public ApiResponse<Map<String, Object>> queryRecommendations(@RequestBody(required = false) AiInvokeRequest request) {
        currentUserAccessor.requireUserId();
        throw new BusinessException(BusinessErrorCode.NOT_IMPLEMENTED, "智能推荐能力尚未启用，接口已预留");
    }

    @PostMapping("/moderation/check")
    public ApiResponse<AiModerationCheckResponse> checkModeration(@Valid @RequestBody AiModerationCheckRequest request) {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(moderationGatewayService.check(userId, request));
    }

    @PostMapping("/customer-service/sessions")
    public ApiResponse<Map<String, Object>> createCustomerServiceSession(@RequestBody(required = false) AiInvokeRequest request) {
        currentUserAccessor.requireUserId();
        return ApiResponse.success(Map.of(
                "sessionId", "cs_" + UUID.randomUUID().toString().replace("-", ""),
                "status", "RESERVED",
                "message", "智能客服能力尚未启用，接口已预留"
        ));
    }

    @PostMapping("/analytics/query")
    public ApiResponse<Map<String, Object>> queryAnalytics(@RequestBody(required = false) AiInvokeRequest request) {
        currentUserAccessor.requireUserId();
        throw new BusinessException(BusinessErrorCode.NOT_IMPLEMENTED, "AI 数据分析能力尚未启用，接口已预留");
    }
}
