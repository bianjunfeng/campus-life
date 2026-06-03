package com.campus.campus_life_ai.ai.provider;

import io.micrometer.observation.ObservationRegistry;
import io.netty.channel.ChannelOption;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.openai-compatible.client", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiOpenAiCompatibleProvider implements LlmProvider {

    private static final String PROVIDER_CODE = "openai-compatible";
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean isAvailable(ProviderRuntimeConfig runtimeConfig) {
        return runtimeConfig != null
                && Boolean.TRUE.equals(runtimeConfig.getEnabled())
                && StringUtils.hasText(runtimeConfig.getBaseUrl())
                && StringUtils.hasText(runtimeConfig.getApiKey())
                && StringUtils.hasText(runtimeConfig.getDefaultModelCode());
    }

    @Override
    public ChatCompletionResult chat(ChatCompletionCommand command, ProviderRuntimeConfig runtimeConfig) {
        ensureAvailable(runtimeConfig);

        OpenAiChatModel chatModel = buildChatModel(runtimeConfig);
        Prompt prompt = buildPrompt(command, false);
        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        int latency = (int) (System.currentTimeMillis() - start);
        return toResult(response, runtimeConfig, latency);
    }

    @Override
    public ChatCompletionResult stream(ChatCompletionCommand command,
                                       ProviderRuntimeConfig runtimeConfig,
                                       ChatCompletionStreamConsumer consumer) {
        ensureAvailable(runtimeConfig);

        OpenAiChatModel chatModel = buildChatModel(runtimeConfig);
        Prompt prompt = buildPrompt(command, true);
        StringBuilder content = new StringBuilder();
        ChatResponse lastResponse = null;
        long start = System.currentTimeMillis();

        for (ChatResponse response : chatModel.stream(prompt).toIterable()) {
            lastResponse = response;
            String delta = extractContent(response);
            if (StringUtils.hasText(delta)) {
                content.append(delta);
                if (consumer != null) {
                    consumer.onDelta(delta);
                }
            }
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务未返回有效内容");
        }

        int latency = (int) (System.currentTimeMillis() - start);
        return toResult(lastResponse, runtimeConfig, latency, content.toString());
    }

    private OpenAiChatModel buildChatModel(ProviderRuntimeConfig runtimeConfig) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(resolveTimeout(runtimeConfig.getConnectTimeoutMs(), 5000));
        requestFactory.setReadTimeout(resolveTimeout(runtimeConfig.getReadTimeoutMs(), 30000));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, resolveTimeout(runtimeConfig.getConnectTimeoutMs(), 5000))
                .responseTimeout(Duration.ofMillis(resolveTimeout(runtimeConfig.getReadTimeoutMs(), 30000)));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(runtimeConfig.getBaseUrl()))
                .apiKey(runtimeConfig.getApiKey())
                .completionsPath(CHAT_COMPLETIONS_PATH)
                .embeddingsPath(EMBEDDINGS_PATH)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(buildOptions(runtimeConfig, false))
                .toolCallingManager(DefaultToolCallingManager.builder()
                        .observationRegistry(ObservationRegistry.NOOP)
                        .build())
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    private Prompt buildPrompt(ChatCompletionCommand command, boolean stream) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(command.getSystemPrompt())) {
            messages.add(new SystemMessage(command.getSystemPrompt()));
        }

        if (command.getMessages() != null) {
            for (ChatCompletionCommand.PromptMessage message : command.getMessages()) {
                if (message == null || !StringUtils.hasText(message.getContent())) {
                    continue;
                }
                messages.add(toSpringMessage(message));
            }
        }

        return new Prompt(messages, buildOptions(command, stream));
    }

    private OpenAiChatOptions buildOptions(ChatCompletionCommand command, boolean stream) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(command.getModelCode())
                .temperature(command.getTemperature())
                .streamUsage(stream);
        if (command.getMaxOutputTokens() != null && command.getMaxOutputTokens() > 0) {
            builder.maxTokens(command.getMaxOutputTokens());
        }
        List<ToolCallback> toolCallbacks = command.getToolCallbacks();
        if (toolCallbacks != null && !toolCallbacks.isEmpty()) {
            builder.toolCallbacks(toolCallbacks)
                    .internalToolExecutionEnabled(true)
                    .parallelToolCalls(false);
        }
        Map<String, Object> toolContext = command.getToolContext();
        if (toolContext != null && !toolContext.isEmpty()) {
            builder.toolContext(toolContext);
        }
        return builder.build();
    }

    private OpenAiChatOptions buildOptions(ProviderRuntimeConfig runtimeConfig, boolean stream) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(runtimeConfig.getDefaultModelCode())
                .temperature(runtimeConfig.getTemperature())
                .streamUsage(stream);
        if (runtimeConfig.getMaxOutputTokens() != null && runtimeConfig.getMaxOutputTokens() > 0) {
            builder.maxTokens(runtimeConfig.getMaxOutputTokens());
        }
        return builder.build();
    }

    private Message toSpringMessage(ChatCompletionCommand.PromptMessage message) {
        String role = message.getRole() == null ? "user" : message.getRole().trim().toLowerCase();
        return switch (role) {
            case "assistant" -> new AssistantMessage(message.getContent());
            case "system" -> new SystemMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }

    private ChatCompletionResult toResult(ChatResponse response,
                                          ProviderRuntimeConfig runtimeConfig,
                                          int latency) {
        String content = extractContent(response);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务未返回有效内容");
        }
        return toResult(response, runtimeConfig, latency, content);
    }

    private ChatCompletionResult toResult(ChatResponse response,
                                          ProviderRuntimeConfig runtimeConfig,
                                          int latency,
                                          String content) {
        Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
        Generation generation = response == null ? null : response.getResult();
        String modelCode = response == null || response.getMetadata() == null
                ? runtimeConfig.getDefaultModelCode()
                : response.getMetadata().getModel();
        if (!StringUtils.hasText(modelCode)) {
            modelCode = runtimeConfig.getDefaultModelCode();
        }

        return ChatCompletionResult.builder()
                .content(content)
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(modelCode)
                .promptTokens(usage == null ? null : usage.getPromptTokens())
                .completionTokens(usage == null ? null : usage.getCompletionTokens())
                .totalTokens(usage == null ? null : usage.getTotalTokens())
                .latencyMs(latency)
                .finishReason(generation == null || generation.getMetadata() == null
                        ? null
                        : generation.getMetadata().getFinishReason())
                .build();
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
    }

    private void ensureAvailable(ProviderRuntimeConfig runtimeConfig) {
        if (!isAvailable(runtimeConfig)) {
            throw new IllegalStateException("未启用可用的大模型供应商配置");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }

    private int resolveTimeout(Integer timeoutMs, int fallbackMs) {
        return timeoutMs == null || timeoutMs <= 0 ? fallbackMs : timeoutMs;
    }
}
