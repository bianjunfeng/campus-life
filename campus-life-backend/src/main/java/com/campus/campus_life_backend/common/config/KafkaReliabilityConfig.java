package com.campus.campus_life_backend.common.config;

import com.campus.campus_life_backend.modules.search.service.SearchOpsMetricsService;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaReliabilityConfig {

    private final ConcurrentKafkaListenerContainerFactoryConfigurer configurer;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SearchOpsMetricsService searchOpsMetricsService;

    public KafkaReliabilityConfig(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            KafkaTemplate<String, String> kafkaTemplate,
            SearchOpsMetricsService searchOpsMetricsService
    ) {
        this.configurer = configurer;
        this.kafkaTemplate = kafkaTemplate;
        this.searchOpsMetricsService = searchOpsMetricsService;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> searchKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Value("${search.kafka.retry.interval-ms:1000}") long intervalMs,
            @Value("${search.kafka.retry.max-attempts:3}") long maxAttempts,
            @Value("${search.kafka.dlt-suffix:.DLT}") String dltSuffix
    ) {
        return buildFactory(consumerFactory, intervalMs, maxAttempts, dltSuffix, SearchOpsMetricsService.CHANNEL_POST_SEARCH);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> notificationKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Value("${notification.kafka.retry.interval-ms:1000}") long intervalMs,
            @Value("${notification.kafka.retry.max-attempts:3}") long maxAttempts,
            @Value("${notification.kafka.dlt-suffix:.DLT}") String dltSuffix
    ) {
        return buildFactory(consumerFactory, intervalMs, maxAttempts, dltSuffix, SearchOpsMetricsService.CHANNEL_NOTIFICATION);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> userSearchKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Value("${search.kafka.retry.interval-ms:1000}") long intervalMs,
            @Value("${search.kafka.retry.max-attempts:3}") long maxAttempts,
            @Value("${search.kafka.dlt-suffix:.DLT}") String dltSuffix
    ) {
        return buildFactory(consumerFactory, intervalMs, maxAttempts, dltSuffix, SearchOpsMetricsService.CHANNEL_USER_SEARCH);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> voucherOrderKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Value("${voucher.order.kafka.retry.interval-ms:1000}") long intervalMs,
            @Value("${voucher.order.kafka.retry.max-attempts:3}") long maxAttempts,
            @Value("${voucher.order.kafka.dlt-suffix:.DLT}") String dltSuffix
    ) {
        return buildFactory(consumerFactory, intervalMs, maxAttempts, dltSuffix, SearchOpsMetricsService.CHANNEL_VOUCHER_ORDER);
    }

    @Bean
    @ConditionalOnProperty(prefix = "search.kafka", name = "enabled", havingValue = "true")
    public NewTopic postSearchTopic(
            @Value("${search.kafka.topic:forum-post-search-sync}") String topic,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "search.kafka", name = "enabled", havingValue = "true")
    public NewTopic postSearchDltTopic(
            @Value("${search.kafka.topic:forum-post-search-sync}") String topic,
            @Value("${search.kafka.dlt-suffix:.DLT}") String dltSuffix,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic + dltSuffix, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "search.kafka", name = "enabled", havingValue = "true")
    public NewTopic userSearchTopic(
            @Value("${search.kafka.user-topic:forum-user-search-sync}") String topic,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "search.kafka", name = "enabled", havingValue = "true")
    public NewTopic userSearchDltTopic(
            @Value("${search.kafka.user-topic:forum-user-search-sync}") String topic,
            @Value("${search.kafka.dlt-suffix:.DLT}") String dltSuffix,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic + dltSuffix, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "notification.kafka", name = "enabled", havingValue = "true")
    public NewTopic systemNotificationTopic(
            @Value("${notification.kafka.topic:system-notification-events}") String topic,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "notification.kafka", name = "enabled", havingValue = "true")
    public NewTopic systemNotificationDltTopic(
            @Value("${notification.kafka.topic:system-notification-events}") String topic,
            @Value("${notification.kafka.dlt-suffix:.DLT}") String dltSuffix,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic + dltSuffix, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "voucher.order.kafka", name = "enabled", havingValue = "true")
    public NewTopic voucherOrderEventTopic(
            @Value("${voucher.order.kafka.topic:voucher-order-events}") String topic,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic, partitions, replicas);
    }

    @Bean
    @ConditionalOnProperty(prefix = "voucher.order.kafka", name = "enabled", havingValue = "true")
    public NewTopic voucherOrderEventDltTopic(
            @Value("${voucher.order.kafka.topic:voucher-order-events}") String topic,
            @Value("${voucher.order.kafka.dlt-suffix:.DLT}") String dltSuffix,
            @Value("${event.kafka.topic.partitions:1}") int partitions,
            @Value("${event.kafka.topic.replicas:1}") short replicas
    ) {
        return buildTopic(topic + dltSuffix, partitions, replicas);
    }

    private NewTopic buildTopic(String topic, int partitions, short replicas) {
        return TopicBuilder.name(topic)
                .partitions(Math.max(partitions, 1))
                .replicas(Math.max(replicas, (short) 1))
                .build();
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> buildFactory(
            ConsumerFactory<String, String> consumerFactory,
            long intervalMs,
            long maxAttempts,
            String dltSuffix,
            String channel
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        @SuppressWarnings("unchecked")
        ConsumerFactory<Object, Object> typedConsumerFactory =
                (ConsumerFactory<Object, Object>) (ConsumerFactory<?, ?>) consumerFactory;
        configurer.configure(factory, typedConsumerFactory);
        factory.setCommonErrorHandler(buildErrorHandler(intervalMs, maxAttempts, dltSuffix, channel));
        @SuppressWarnings("unchecked")
        ConcurrentKafkaListenerContainerFactory<String, String> typedFactory =
                (ConcurrentKafkaListenerContainerFactory<String, String>) (ConcurrentKafkaListenerContainerFactory<?, ?>) factory;
        return typedFactory;
    }

    private DefaultErrorHandler buildErrorHandler(long intervalMs, long maxAttempts, String dltSuffix, String channel) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    String dltTopic = record.topic() + dltSuffix;
                    searchOpsMetricsService.recordDeadLetter(
                            channel,
                            record.topic(),
                            dltTopic,
                            record.key() == null ? null : String.valueOf(record.key()),
                            record.value() == null ? null : String.valueOf(record.value()),
                            ex
                    );
                    return new TopicPartition(dltTopic, record.partition());
                }
        );
        long retryCount = Math.max(maxAttempts, 1) - 1;
        FixedBackOff backOff = new FixedBackOff(Math.max(intervalMs, 0), retryCount);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
