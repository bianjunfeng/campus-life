package com.campus.campus_life_ai.ai.gateway.policy;

import com.campus.campus_life_ai.ai.entity.AiCapabilityConfig;
import com.campus.campus_life_ai.ai.entity.AiSceneConfig;
import com.campus.campus_life_ai.ai.gateway.GatewayErrorCode;
import com.campus.campus_life_ai.ai.gateway.dto.GatewayRequest;
import com.campus.campus_life_ai.ai.mapper.AiCapabilityConfigMapper;
import com.campus.campus_life_ai.ai.mapper.AiSceneConfigMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(30)
public class ConfiguredCapabilityScenePolicy implements GatewayPolicy {

    private final AiCapabilityConfigMapper aiCapabilityConfigMapper;
    private final AiSceneConfigMapper aiSceneConfigMapper;

    public ConfiguredCapabilityScenePolicy(AiCapabilityConfigMapper aiCapabilityConfigMapper,
                                           AiSceneConfigMapper aiSceneConfigMapper) {
        this.aiCapabilityConfigMapper = aiCapabilityConfigMapper;
        this.aiSceneConfigMapper = aiSceneConfigMapper;
    }

    @Override
    public void validate(GatewayRequest request) {
        if (request == null) {
            return;
        }

        AiCapabilityConfig capabilityConfig = findCapability(request.getCapabilityCode());
        if (capabilityConfig != null && !isEnabled(capabilityConfig.getEnabled())) {
            throw new GatewayPolicyException(
                    GatewayErrorCode.CAPABILITY_DISABLED,
                    "AI 能力未启用: " + capabilityConfig.getCapabilityCode()
            );
        }

        AiSceneConfig sceneConfig = findScene(request.getSceneCode());
        if (sceneConfig == null) {
            return;
        }
        if (!isEnabled(sceneConfig.getEnabled())) {
            throw new GatewayPolicyException(
                    GatewayErrorCode.SCENE_DISABLED,
                    "AI 场景未启用: " + sceneConfig.getSceneCode()
            );
        }
        if (StringUtils.hasText(request.getCapabilityCode())
                && StringUtils.hasText(sceneConfig.getCapabilityCode())
                && !request.getCapabilityCode().trim().equals(sceneConfig.getCapabilityCode().trim())) {
            throw new GatewayPolicyException(
                    GatewayErrorCode.INVALID_REQUEST,
                    "AI 场景与能力不匹配: " + sceneConfig.getSceneCode()
            );
        }
    }

    private AiCapabilityConfig findCapability(String capabilityCode) {
        return StringUtils.hasText(capabilityCode)
                ? aiCapabilityConfigMapper.findByCapabilityCode(capabilityCode.trim())
                : null;
    }

    private AiSceneConfig findScene(String sceneCode) {
        return StringUtils.hasText(sceneCode)
                ? aiSceneConfigMapper.findBySceneCode(sceneCode.trim())
                : null;
    }

    private boolean isEnabled(Integer enabled) {
        return enabled != null && enabled == 1;
    }
}
