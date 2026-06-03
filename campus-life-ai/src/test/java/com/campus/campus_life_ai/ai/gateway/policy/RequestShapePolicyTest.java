package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.provider.ChatCompletionCommand;
import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestShapePolicyTest {

    @Test
    void shouldRejectPromptLongerThanConfiguredLimit() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.getGateway().setMaxInputChars(5);
        RequestShapePolicy policy = new RequestShapePolicy(properties);

        GatewayPolicyException exception = assertThrows(GatewayPolicyException.class, () -> policy.validate(
                GatewayRequest.builder()
                        .requestId("req-1")
                        .runtimeConfig(ProviderRuntimeConfig.builder().providerCode("provider").build())
                        .command(ChatCompletionCommand.builder()
                                .messages(List.of(ChatCompletionCommand.PromptMessage.builder()
                                        .role("user")
                                        .content("123456")
                                        .build()))
                                .build())
                        .build()
        ));

        assertEquals("INVALID_REQUEST", exception.getError().getErrorCode());
    }
}
