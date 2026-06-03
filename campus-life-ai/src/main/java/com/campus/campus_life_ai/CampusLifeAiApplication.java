package com.campus.campus_life_ai;

import com.campus.campus_life_ai.common.properties.AiProviderProperties;
import com.campus.campus_life_ai.common.properties.AiSecurityProperties;
import com.campus.campus_life_ai.common.properties.OpsAgentProperties;
import com.campus.campus_life_ai.common.properties.PlatformServiceProperties;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeEmbeddingProperties;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeStorageProperties;
import com.campus.campus_life_ai.knowledge.properties.KnowledgeVectorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AiProviderProperties.class,
        AiSecurityProperties.class,
        PlatformServiceProperties.class,
        KnowledgeStorageProperties.class,
        KnowledgeEmbeddingProperties.class,
        KnowledgeVectorProperties.class,
        OpsAgentProperties.class
})
public class CampusLifeAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLifeAiApplication.class, args);
    }
}
