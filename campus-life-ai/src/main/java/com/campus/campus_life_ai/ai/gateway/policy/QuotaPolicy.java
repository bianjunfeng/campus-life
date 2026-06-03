package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.entity.AiUsageQuota;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.mapper.AiUsageQuotaMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Order(50)
public class QuotaPolicy implements GatewayPolicy {

    private final AiUsageQuotaMapper aiUsageQuotaMapper;
    private final AiCallLogMapper aiCallLogMapper;

    public QuotaPolicy(AiUsageQuotaMapper aiUsageQuotaMapper, AiCallLogMapper aiCallLogMapper) {
        this.aiUsageQuotaMapper = aiUsageQuotaMapper;
        this.aiCallLogMapper = aiCallLogMapper;
    }

    @Override
    public void validate(GatewayRequest request) {
        if (request == null) {
            return;
        }
        List<AiUsageQuota> quotas = aiUsageQuotaMapper.findEnabled();
        if (quotas == null || quotas.isEmpty()) {
            return;
        }
        for (AiUsageQuota quota : quotas) {
            if (!matches(quota, request)) {
                continue;
            }
            Map<String, Object> usage = aiCallLogMapper.aggregateQuotaUsage(
                    periodStart(quota.getQuotaPeriod()),
                    userFilter(quota, request),
                    normalizeOptional(quota.getCapabilityCode()),
                    normalizeOptional(quota.getSceneCode())
            );
            rejectIfExceeded(quota, usage);
        }
    }

    private boolean matches(AiUsageQuota quota, GatewayRequest request) {
        if (quota == null) {
            return false;
        }
        if (!matchesCode(quota.getCapabilityCode(), request.getCapabilityCode())
                || !matchesCode(quota.getSceneCode(), request.getSceneCode())) {
            return false;
        }
        String subjectType = normalize(quota.getSubjectType());
        if ("GLOBAL".equals(subjectType)) {
            return true;
        }
        if ("USER".equals(subjectType) && request.getUserId() != null) {
            return request.getUserId().toString().equals(normalizeOptional(quota.getSubjectId()));
        }
        return false;
    }

    private Long userFilter(AiUsageQuota quota, GatewayRequest request) {
        return "USER".equals(normalize(quota.getSubjectType())) ? request.getUserId() : null;
    }

    private boolean matchesCode(String configured, String requested) {
        return !StringUtils.hasText(configured)
                || (StringUtils.hasText(requested) && configured.trim().equals(requested.trim()));
    }

    private void rejectIfExceeded(AiUsageQuota quota, Map<String, Object> usage) {
        long calls = number(usage, "totalCalls").longValue();
        long tokens = number(usage, "totalTokens").longValue();
        BigDecimal cost = decimal(usage, "totalCostAmount");
        if (quota.getMaxCalls() != null && calls >= quota.getMaxCalls()) {
            reject(quota, "调用数");
        }
        if (quota.getMaxTokens() != null && tokens >= quota.getMaxTokens()) {
            reject(quota, "Token");
        }
        if (quota.getMaxCost() != null && cost.compareTo(quota.getMaxCost()) >= 0) {
            reject(quota, "成本");
        }
    }

    private LocalDateTime periodStart(String period) {
        LocalDate today = LocalDate.now();
        if ("MONTH".equals(normalize(period))) {
            return today.withDayOfMonth(1).atStartOfDay();
        }
        return today.atStartOfDay();
    }

    private Number number(Map<String, Object> values, String key) {
        Object value = value(values, key);
        return value instanceof Number number ? number : 0;
    }

    private BigDecimal decimal(Map<String, Object> values, String key) {
        Object value = value(values, key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private Object value(Map<String, Object> values, String key) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.containsKey(key)) {
            return values.get(key);
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void reject(AiUsageQuota quota, String dimension) {
        throw new GatewayPolicyException(
                GatewayErrorCode.QUOTA_EXCEEDED,
                "AI 使用配额已达上限: " + dimension + " / " + quota.getQuotaPeriod()
        );
    }
}
