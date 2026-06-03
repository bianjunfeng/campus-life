package com.campus.campus_life_ai.ai.service;

import com.campus.campus_life_ai.ai.entity.AiProviderConfig;
import com.campus.campus_life_ai.ai.mapper.AiProviderConfigMapper;
import com.campus.campus_life_ai.common.security.ApiKeyCipherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class AiProviderSecretMigrationRunner implements ApplicationRunner {

    private final AiProviderConfigMapper aiProviderConfigMapper;
    private final ApiKeyCipherService apiKeyCipherService;

    public AiProviderSecretMigrationRunner(AiProviderConfigMapper aiProviderConfigMapper,
                                           ApiKeyCipherService apiKeyCipherService) {
        this.aiProviderConfigMapper = aiProviderConfigMapper;
        this.apiKeyCipherService = apiKeyCipherService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!apiKeyCipherService.isConfigured()) {
            log.warn("未配置 AI_API_KEY_ENCRYPTION_SECRET，跳过 ai_provider_config 历史密钥迁移");
            return;
        }

        List<AiProviderConfig> providers = aiProviderConfigMapper.findAll();
        int migrated = 0;
        for (AiProviderConfig provider : providers) {
            if (!StringUtils.hasText(provider.getApiKeyCipher()) || apiKeyCipherService.isEncrypted(provider.getApiKeyCipher())) {
                continue;
            }
            provider.setApiKeyCipher(apiKeyCipherService.encrypt(provider.getApiKeyCipher()));
            provider.setUpdatedAt(LocalDateTime.now());
            aiProviderConfigMapper.update(provider);
            migrated++;
        }

        if (migrated > 0) {
            log.info("已完成 ai_provider_config 历史 API Key 加密迁移, count={}", migrated);
        }
    }
}
