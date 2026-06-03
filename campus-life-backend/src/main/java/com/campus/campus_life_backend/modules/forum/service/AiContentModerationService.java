package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.forum.config.AiModerationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiContentModerationService {

    private static final Logger log = LoggerFactory.getLogger(AiContentModerationService.class);
    private static final String CHECK_PATH = "/api/ai/moderation/check";
    private static final String RESULT_REJECT = "REJECT";
    private static final String RESULT_REVIEW = "REVIEW";

    private final RestTemplate restTemplate;
    private final AiModerationProperties properties;

    public AiContentModerationService(RestTemplate restTemplate, AiModerationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void checkPost(Long userId, String title, String content) {
        check(userId, "post", joinTitleAndContent(title, content));
    }

    public void checkComment(Long userId, String content) {
        check(userId, "comment", content);
    }

    private void check(Long userId, String targetType, String content) {
        if (!properties.isEnabled() || !StringUtils.hasText(content)) {
            return;
        }

        try {
            ModerationDecision decision = requestModeration(targetType, content);
            enforceDecision(targetType, decision);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            if (properties.isFailOpen()) {
                log.warn("AI 内容审核不可用，已按 fail-open 放行: userId={}, targetType={}, message={}",
                        userId, targetType, e.getMessage());
                return;
            }
            throw new BusinessException(BusinessErrorCode.SERVICE_UNAVAILABLE, "AI 内容审核暂不可用，请稍后重试", e);
        }
    }

    private ModerationDecision requestModeration(String targetType, String content) {
        String authorization = currentAuthorizationHeader();
        if (!StringUtils.hasText(authorization)) {
            throw new IllegalStateException("缺少访问令牌，无法调用 AI 内容审核");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        Map<String, Object> payload = Map.of(
                "content", content,
                "targetType", targetType,
                "sceneCode", "moderation.text_post"
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    moderationUrl(),
                    new HttpEntity<>(payload, headers),
                    JsonNode.class
            );
            return parseDecision(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("AI 内容审核请求失败: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("AI 内容审核请求异常", e);
        }
    }

    private ModerationDecision parseDecision(JsonNode body) {
        if (body == null || body.isMissingNode() || body.isNull()) {
            throw new IllegalStateException("AI 内容审核返回为空");
        }
        int code = body.path("code").asInt(500);
        if (code != 200) {
            String message = body.path("message").asText("AI 内容审核返回失败");
            throw new IllegalStateException(message);
        }

        JsonNode data = body.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new IllegalStateException("AI 内容审核缺少结果数据");
        }
        return new ModerationDecision(
                data.path("result").asText(RESULT_REVIEW),
                data.path("score").isNumber() ? data.path("score").asDouble() : null,
                categories(data.path("categories")),
                data.path("reason").asText("")
        );
    }

    private List<String> categories(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> categories = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText(null);
            if (StringUtils.hasText(value)) {
                categories.add(value.trim());
            }
        }
        return categories;
    }

    private void enforceDecision(String targetType, ModerationDecision decision) {
        String result = decision.result() == null ? RESULT_REVIEW : decision.result().trim().toUpperCase();
        if (RESULT_REJECT.equals(result) || (RESULT_REVIEW.equals(result) && properties.isBlockReview())) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_PARAM,
                    buildRejectMessage(targetType, decision)
            );
        }
    }

    private String buildRejectMessage(String targetType, ModerationDecision decision) {
        String typeName = "comment".equals(targetType) ? "评论" : "帖子";
        String reason = StringUtils.hasText(decision.reason()) ? "：" + decision.reason().trim() : "";
        return typeName + "内容未通过 AI 审核，请修改后再发布" + reason;
    }

    private String joinTitleAndContent(String title, String content) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(title)) {
            builder.append(title.trim());
        }
        if (StringUtils.hasText(content)) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(content.trim());
        }
        return builder.toString();
    }

    private String moderationUrl() {
        String baseUrl = StringUtils.hasText(properties.getBaseUrl())
                ? properties.getBaseUrl().trim()
                : "http://127.0.0.1:8083";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + CHECK_PATH;
    }

    private String currentAuthorizationHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request == null ? null : request.getHeader(HttpHeaders.AUTHORIZATION);
    }

    record ModerationDecision(String result, Double score, List<String> categories, String reason) {
    }
}
