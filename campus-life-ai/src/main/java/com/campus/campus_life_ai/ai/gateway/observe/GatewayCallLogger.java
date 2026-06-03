package com.campus.campus_life_ai.ai.gateway.observe;

import com.campus.campus_life_ai.ai.entity.AiCallLog;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayResponse;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayUsage;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
public class GatewayCallLogger {

    private final AiCallLogMapper aiCallLogMapper;

    public GatewayCallLogger(AiCallLogMapper aiCallLogMapper) {
        this.aiCallLogMapper = aiCallLogMapper;
    }

    public void recordSuccess(GatewayRequest request, GatewayResponse response) {
        GatewayUsage usage = response.getUsage();
        AiCallLog callLog = newBaseLog(request);
        callLog.setProviderCode(resolve(response.getProviderCode(), request.getProviderCode()));
        callLog.setModelCode(resolve(response.getModelCode(), request.getModelCode()));
        callLog.setSuccess(1);
        callLog.setLatencyMs(response.getLatencyMs());
        callLog.setPromptTokens(usage == null ? null : usage.getPromptTokens());
        callLog.setCompletionTokens(usage == null ? null : usage.getCompletionTokens());
        callLog.setTotalTokens(usage == null ? null : usage.getTotalTokens());
        callLog.setFallbackLevel(response.getFallbackLevel());
        callLog.setPromptChars(promptChars(request));
        callLog.setCompletionChars(length(response.getContent()));
        callLog.setCostAmount(usage == null ? null : usage.getCostAmount());
        aiCallLogMapper.insert(callLog);
    }

    public void recordFailure(GatewayRequest request, GatewayError error) {
        AiCallLog callLog = newBaseLog(request);
        callLog.setProviderCode(request == null ? null : request.getProviderCode());
        callLog.setModelCode(request == null ? null : request.getModelCode());
        callLog.setSuccess(0);
        callLog.setPromptChars(promptChars(request));
        callLog.setErrorCode(error == null ? null : error.getErrorCode());
        callLog.setErrorType(error == null ? null : error.getErrorType());
        callLog.setErrorMessage(abbreviate(error == null ? null : error.getMessage(), 512));
        aiCallLogMapper.insert(callLog);
    }

    private AiCallLog newBaseLog(GatewayRequest request) {
        AiCallLog callLog = new AiCallLog();
        if (request != null) {
            callLog.setRequestId(request.getRequestId());
            callLog.setSessionId(request.getSessionId());
            callLog.setUserId(request.getUserId());
            callLog.setCapabilityCode(request.getCapabilityCode());
            callLog.setSceneCode(request.getSceneCode());
        }
        callLog.setCreatedAt(LocalDateTime.now());
        return callLog;
    }

    private String resolve(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String abbreviate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private int promptChars(GatewayRequest request) {
        ChatCompletionCommand command = request == null ? null : request.getCommand();
        if (command == null) {
            return 0;
        }
        int chars = length(command.getSystemPrompt());
        if (command.getMessages() == null) {
            return chars;
        }
        for (ChatCompletionCommand.PromptMessage message : command.getMessages()) {
            chars += length(message == null ? null : message.getContent());
        }
        return chars;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
