package com.campus.campus_life_ai.ai.gateway.observe;

import com.campus.campus_life_ai.ai.entity.AiModelConfig;
import com.campus.campus_life_ai.ai.mapper.AiModelConfigMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GatewayCostCalculatorTest {

    private final AiModelConfigMapper aiModelConfigMapper = mock(AiModelConfigMapper.class);
    private final GatewayCostCalculator calculator = new GatewayCostCalculator(aiModelConfigMapper);

    @Test
    void shouldCalculateCostFromModelTokenPrices() {
        AiModelConfig model = new AiModelConfig();
        model.setInputPricePer1k(new BigDecimal("0.010000"));
        model.setOutputPricePer1k(new BigDecimal("0.020000"));
        given(aiModelConfigMapper.findByProviderAndModelCode("provider-a", "model-a")).willReturn(model);

        BigDecimal cost = calculator.calculate("provider-a", "model-a", 1500, 250);

        assertEquals(new BigDecimal("0.020000"), cost);
    }
}
