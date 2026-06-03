package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@Order(20)
public class RequestShapePolicy implements GatewayPolicy {

    private final AiProviderProperties aiProviderProperties;

    public RequestShapePolicy(AiProviderProperties aiProviderProperties) {
        this.aiProviderProperties = aiProviderProperties;
    }

    @Override
    public void validate(GatewayRequest request) {
        if (request == null) {
            reject("网关请求不能为空");
        }
        if (!StringUtils.hasText(request.getRequestId())) {
            reject("网关请求缺少 requestId");
        }
        if (request.getRuntimeConfig() == null) {
            reject("网关请求缺少 Provider 运行时配置");
        }
        ChatCompletionCommand command = request.getCommand();
        if (command == null || CollectionUtils.isEmpty(command.getMessages())) {
            reject("网关请求缺少提示消息");
        }
        if (command.getMaxOutputTokens() != null && command.getMaxOutputTokens() <= 0) {
            reject("maxOutputTokens 必须大于 0");
        }
        if (command.getTemperature() != null
                && (command.getTemperature() < 0D || command.getTemperature() > 2D)) {
            reject("temperature 必须在 0 到 2 之间");
        }

        int inputChars = length(command.getSystemPrompt());
        boolean hasPromptContent = StringUtils.hasText(command.getSystemPrompt());
        for (ChatCompletionCommand.PromptMessage message : command.getMessages()) {
            if (message == null) {
                continue;
            }
            inputChars += length(message.getContent());
            hasPromptContent = hasPromptContent || StringUtils.hasText(message.getContent());
        }
        if (!hasPromptContent) {
            reject("网关请求提示内容不能为空");
        }

        int maxInputChars = aiProviderProperties.getGateway().getMaxInputChars();
        if (maxInputChars > 0 && inputChars > maxInputChars) {
            reject("网关请求输入长度超过限制: " + maxInputChars);
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private void reject(String message) {
        throw new GatewayPolicyException(GatewayErrorCode.INVALID_REQUEST, message);
    }
}
