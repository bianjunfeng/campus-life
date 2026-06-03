package com.campus.campus_life_backend.modules.file.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "file.storage.type", havingValue = "oss")
public class OssClientConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(
            @Value("${file.oss.endpoint}") String endpoint,
            @Value("${file.oss.access-key-id}") String accessKeyId,
            @Value("${file.oss.access-key-secret}") String accessKeySecret,
            @Value("${file.oss.client.max-connections:200}") int maxConnections,
            @Value("${file.oss.client.connection-timeout-ms:5000}") int connectionTimeoutMs,
            @Value("${file.oss.client.socket-timeout-ms:10000}") int socketTimeoutMs,
            @Value("${file.oss.client.max-error-retry:2}") int maxErrorRetry) {
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setMaxConnections(maxConnections);
        configuration.setConnectionTimeout(connectionTimeoutMs);
        configuration.setSocketTimeout(socketTimeoutMs);
        configuration.setMaxErrorRetry(maxErrorRetry);

        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, configuration);
    }
}
