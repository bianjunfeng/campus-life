package com.campus.campus_life_ai.ai.provider;

import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "ai.openai-compatible.client", havingValue = "legacy")
public class OpenAiCompatibleProvider implements LlmProvider {

    private final AiProviderProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleProvider(AiProviderProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerCode() {
        return properties.getOpenaiCompatible().getProviderCode();
    }

    @Override
    public boolean isAvailable(ProviderRuntimeConfig runtimeConfig) {
        return runtimeConfig != null
                && Boolean.TRUE.equals(runtimeConfig.getEnabled())
                && StringUtils.hasText(runtimeConfig.getBaseUrl())
                && StringUtils.hasText(runtimeConfig.getApiKey());
    }

    @Override
    public ChatCompletionResult chat(ChatCompletionCommand command, ProviderRuntimeConfig runtimeConfig) {
        if (!isAvailable(runtimeConfig)) {
            throw new IllegalStateException("未启用可用的大模型供应商配置");
        }

        RestClient restClient = buildClient(runtimeConfig);

        String effectiveModel = StringUtils.hasText(command.getModelCode()) ? command.getModelCode() : runtimeConfig.getDefaultModelCode();
        boolean streamMode = requiresStreamMode(effectiveModel);
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", effectiveModel);
        payload.put("temperature", command.getTemperature() == null ? properties.getTemperature() : command.getTemperature());
        payload.put("stream", streamMode);
        if (streamMode) {
            payload.put("stream_options", Map.of("include_usage", true));
        }
        if (command.getMaxOutputTokens() != null && command.getMaxOutputTokens() > 0) {
            payload.put("max_tokens", command.getMaxOutputTokens());
        }
        payload.put("messages", buildMessages(command));

        long start = System.currentTimeMillis();
        String responseBody = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + runtimeConfig.getApiKey())
                .body(payload)
                .retrieve()
                .body(String.class);
        int latency = (int) (System.currentTimeMillis() - start);

        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("模型服务返回空响应");
        }

        if (streamMode) {
            return parseStreamingResponse(responseBody, runtimeConfig.getProviderCode(), effectiveModel, latency);
        }

        JsonNode response = readJson(responseBody);
        JsonNode firstChoice = response.path("choices").isArray() && response.path("choices").size() > 0
                ? response.path("choices").get(0)
                : null;
        String content = firstChoice == null ? null : firstChoice.path("message").path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务未返回有效内容");
        }

        JsonNode usage = response.path("usage");
        return ChatCompletionResult.builder()
                .content(content)
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(response.path("model").asText(payload.get("model").toString()))
                .promptTokens(readInt(usage, "prompt_tokens"))
                .completionTokens(readInt(usage, "completion_tokens"))
                .totalTokens(readInt(usage, "total_tokens"))
                .latencyMs(latency)
                .finishReason(firstChoice == null ? null : firstChoice.path("finish_reason").asText(null))
                .build();
    }

    @Override
    public ChatCompletionResult stream(ChatCompletionCommand command,
                                       ProviderRuntimeConfig runtimeConfig,
                                       ChatCompletionStreamConsumer consumer) {
        if (!isAvailable(runtimeConfig)) {
            throw new IllegalStateException("未启用可用的大模型供应商配置");
        }

        String effectiveModel = StringUtils.hasText(command.getModelCode()) ? command.getModelCode() : runtimeConfig.getDefaultModelCode();
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", effectiveModel);
        payload.put("temperature", command.getTemperature() == null ? properties.getTemperature() : command.getTemperature());
        payload.put("stream", true);
        payload.put("stream_options", Map.of("include_usage", true));
        if (command.getMaxOutputTokens() != null && command.getMaxOutputTokens() > 0) {
            payload.put("max_tokens", command.getMaxOutputTokens());
        }
        payload.put("messages", buildMessages(command));

        long start = System.currentTimeMillis();
        StringBuilder answerContent = new StringBuilder();
        StringBuilder reasoningContent = new StringBuilder();
        String[] resolvedModel = new String[]{effectiveModel};
        String[] finishReason = new String[1];
        Integer[] usageTokens = new Integer[3];

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(joinUrl(runtimeConfig.getBaseUrl(), "/chat/completions"))
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(runtimeConfig.getConnectTimeoutMs());
            connection.setReadTimeout(runtimeConfig.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("Authorization", "Bearer " + runtimeConfig.getApiKey());

            byte[] body = objectMapper.writeValueAsBytes(payload);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("模型服务调用失败: HTTP " + status + " " + readResponseBody(connection.getErrorStream()));
            }

            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("data:")) {
                        continue;
                    }

                    String data = trimmed.substring(5).trim();
                    if (!StringUtils.hasText(data)) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    JsonNode chunk = readJson(data);
                    if (chunk.hasNonNull("model")) {
                        resolvedModel[0] = chunk.get("model").asText(resolvedModel[0]);
                    }

                    JsonNode usage = chunk.path("usage");
                    usageTokens[0] = readInt(usage, "prompt_tokens", usageTokens[0]);
                    usageTokens[1] = readInt(usage, "completion_tokens", usageTokens[1]);
                    usageTokens[2] = readInt(usage, "total_tokens", usageTokens[2]);

                    JsonNode firstChoice = chunk.path("choices").isArray() && chunk.path("choices").size() > 0
                            ? chunk.path("choices").get(0)
                            : null;
                    if (firstChoice == null) {
                        continue;
                    }

                    JsonNode delta = firstChoice.path("delta");
                    if (delta.hasNonNull("reasoning_content")) {
                        reasoningContent.append(delta.get("reasoning_content").asText(""));
                    }
                    if (delta.hasNonNull("content")) {
                        String deltaContent = delta.get("content").asText("");
                        if (StringUtils.hasText(deltaContent)) {
                            answerContent.append(deltaContent);
                            if (consumer != null) {
                                consumer.onDelta(deltaContent);
                            }
                        }
                    }

                    String chunkFinishReason = firstChoice.path("finish_reason").asText(null);
                    if (StringUtils.hasText(chunkFinishReason) && !"null".equalsIgnoreCase(chunkFinishReason)) {
                        finishReason[0] = chunkFinishReason;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("模型流式调用失败", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        String content = StringUtils.hasText(answerContent.toString())
                ? answerContent.toString()
                : reasoningContent.toString();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务未返回有效内容");
        }

        return ChatCompletionResult.builder()
                .content(content)
                .providerCode(runtimeConfig.getProviderCode())
                .modelCode(resolvedModel[0])
                .promptTokens(usageTokens[0])
                .completionTokens(usageTokens[1])
                .totalTokens(usageTokens[2])
                .latencyMs((int) (System.currentTimeMillis() - start))
                .finishReason(finishReason[0])
                .build();
    }

    private ChatCompletionResult parseStreamingResponse(String responseBody, String providerCode, String fallbackModel, int latency) {
        StringBuilder answerContent = new StringBuilder();
        StringBuilder reasoningContent = new StringBuilder();
        String resolvedModel = fallbackModel;
        String finishReason = null;
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;

        try (BufferedReader reader = new BufferedReader(new StringReader(responseBody))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) {
                    continue;
                }

                String data = trimmed.substring(5).trim();
                if (!StringUtils.hasText(data) || "[DONE]".equals(data)) {
                    continue;
                }

                JsonNode chunk = readJson(data);
                if (chunk.hasNonNull("model")) {
                    resolvedModel = chunk.get("model").asText(resolvedModel);
                }

                JsonNode usage = chunk.path("usage");
                promptTokens = readInt(usage, "prompt_tokens", promptTokens);
                completionTokens = readInt(usage, "completion_tokens", completionTokens);
                totalTokens = readInt(usage, "total_tokens", totalTokens);

                JsonNode firstChoice = chunk.path("choices").isArray() && chunk.path("choices").size() > 0
                        ? chunk.path("choices").get(0)
                        : null;
                if (firstChoice == null) {
                    continue;
                }

                JsonNode delta = firstChoice.path("delta");
                if (delta.hasNonNull("reasoning_content")) {
                    reasoningContent.append(delta.get("reasoning_content").asText(""));
                }
                if (delta.hasNonNull("content")) {
                    answerContent.append(delta.get("content").asText(""));
                }

                String chunkFinishReason = firstChoice.path("finish_reason").asText(null);
                if (StringUtils.hasText(chunkFinishReason) && !"null".equalsIgnoreCase(chunkFinishReason)) {
                    finishReason = chunkFinishReason;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("模型流式响应解析失败", e);
        }

        String content = StringUtils.hasText(answerContent.toString())
                ? answerContent.toString()
                : reasoningContent.toString();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务未返回有效内容");
        }

        return ChatCompletionResult.builder()
                .content(content)
                .providerCode(providerCode)
                .modelCode(resolvedModel)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .latencyMs(latency)
                .finishReason(finishReason)
                .build();
    }

    private RestClient buildClient(ProviderRuntimeConfig runtimeConfig) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(runtimeConfig.getConnectTimeoutMs());
        requestFactory.setReadTimeout(runtimeConfig.getReadTimeoutMs());
        return RestClient.builder()
                .baseUrl(runtimeConfig.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private List<Map<String, Object>> buildMessages(ChatCompletionCommand command) {
        List<Map<String, Object>> promptMessages = new ArrayList<>(command.getMessages().stream()
                .map(message -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("role", message.getRole());
                    item.put("content", message.getContent());
                    return item;
                })
                .toList());
        if (!StringUtils.hasText(command.getSystemPrompt())) {
            return promptMessages;
        }

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", command.getSystemPrompt());
        promptMessages.add(0, systemMessage);
        return promptMessages;
    }

    private Integer readInt(JsonNode usage, String field) {
        return usage != null && usage.has(field) ? usage.get(field).asInt() : null;
    }

    private Integer readInt(JsonNode usage, String field, Integer fallback) {
        Integer value = readInt(usage, field);
        return value == null ? fallback : value;
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException("模型服务响应解析失败", e);
        }
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + path;
    }

    private String readResponseBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private boolean requiresStreamMode(String modelCode) {
        return StringUtils.hasText(modelCode) && modelCode.toLowerCase().startsWith("qvq-");
    }
}
