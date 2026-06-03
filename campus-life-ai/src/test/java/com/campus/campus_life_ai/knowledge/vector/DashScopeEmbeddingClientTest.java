package com.campus.campus_life_ai.knowledge.vector;

import com.campus.campus_life_ai.ai.provider.ProviderRuntimeConfig;
import com.campus.campus_life_ai.ai.service.AiRuntimeConfigService;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeEmbeddingProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DashScopeEmbeddingClientTest {

    @Test
    void shouldUseConfiguredEmbeddingApiKeyFirst() {
        KnowledgeEmbeddingProperties properties = new KnowledgeEmbeddingProperties();
        properties.setApiKey(" embedding-key ");
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        DashScopeEmbeddingClient client = new DashScopeEmbeddingClient(properties, runtimeConfigService);

        assertEquals("embedding-key", client.resolveApiKey());
    }

    @Test
    void shouldFallbackToRuntimeProviderApiKeyWhenEmbeddingKeyMissing() {
        KnowledgeEmbeddingProperties properties = new KnowledgeEmbeddingProperties();
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        given(runtimeConfigService.resolveProvider(null)).willReturn(ProviderRuntimeConfig.builder()
                .providerCode("openai-compatible")
                .apiKey("provider-key")
                .build());
        DashScopeEmbeddingClient client = new DashScopeEmbeddingClient(properties, runtimeConfigService);

        assertEquals("provider-key", client.resolveApiKey());
    }
}
