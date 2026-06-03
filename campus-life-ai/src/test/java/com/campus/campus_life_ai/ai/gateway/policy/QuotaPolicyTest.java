package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.entity.AiUsageQuota;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.mapper.AiCallLogMapper;
import com.campus.campus_life_ai.ai.mapper.AiUsageQuotaMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class QuotaPolicyTest {

    private final AiUsageQuotaMapper usageQuotaMapper = mock(AiUsageQuotaMapper.class);
    private final AiCallLogMapper callLogMapper = mock(AiCallLogMapper.class);
    private final QuotaPolicy policy = new QuotaPolicy(usageQuotaMapper, callLogMapper);

    @Test
    void shouldRejectWhenDailyUserCallQuotaIsReached() {
        AiUsageQuota quota = new AiUsageQuota();
        quota.setSubjectType("USER");
        quota.setSubjectId("1001");
        quota.setCapabilityCode("chat");
        quota.setQuotaPeriod("DAY");
        quota.setMaxCalls(3);
        given(usageQuotaMapper.findEnabled()).willReturn(List.of(quota));
        given(callLogMapper.aggregateQuotaUsage(any(), eq(1001L), eq("chat"), eq(null)))
                .willReturn(Map.of("totalCalls", 3, "totalTokens", 12, "totalCostAmount", 0));

        GatewayPolicyException exception = assertThrows(GatewayPolicyException.class, () -> policy.validate(
                GatewayRequest.builder()
                        .userId(1001L)
                        .capabilityCode("chat")
                        .sceneCode("chat.general")
                        .build()
        ));

        assertEquals("QUOTA_EXCEEDED", exception.getError().getErrorCode());
    }
}
