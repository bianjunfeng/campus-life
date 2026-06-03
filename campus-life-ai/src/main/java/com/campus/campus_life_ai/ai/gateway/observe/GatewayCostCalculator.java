package com.campus.campus_life_ai.ai.gateway.observe;

import com.campus.campus_life_ai.ai.entity.AiModelConfig;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GatewayCostCalculator {

    private static final BigDecimal TOKENS_PER_PRICE_UNIT = BigDecimal.valueOf(1000);

    private final AiModelConfigMapper aiModelConfigMapper;

    public GatewayCostCalculator(AiModelConfigMapper aiModelConfigMapper) {
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    public BigDecimal calculate(String providerCode,
                                String modelCode,
                                Integer promptTokens,
                                Integer completionTokens) {
        if (providerCode == null || modelCode == null) {
            return null;
        }
        AiModelConfig modelConfig = aiModelConfigMapper.findByProviderAndModelCode(providerCode, modelCode);
        if (modelConfig == null
                || (modelConfig.getInputPricePer1k() == null && modelConfig.getOutputPricePer1k() == null)) {
            return null;
        }

        BigDecimal promptCost = cost(promptTokens, modelConfig.getInputPricePer1k());
        BigDecimal completionCost = cost(completionTokens, modelConfig.getOutputPricePer1k());
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal cost(Integer tokens, BigDecimal pricePer1k) {
        if (tokens == null || tokens <= 0 || pricePer1k == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(tokens)
                .multiply(pricePer1k)
                .divide(TOKENS_PER_PRICE_UNIT, 6, RoundingMode.HALF_UP);
    }
}
