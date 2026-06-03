package com.campus.campus_life_backend.modules.voucher.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VoucherRabbitConfig {

    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(VoucherMqConstants.SECKILL_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(VoucherMqConstants.SECKILL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", VoucherMqConstants.SECKILL_RETRY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", VoucherMqConstants.SECKILL_RETRY_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue)
                .to(seckillOrderExchange)
                .with(VoucherMqConstants.SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillRetryExchange() {
        return new DirectExchange(VoucherMqConstants.SECKILL_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillRetryQueue() {
        return QueueBuilder.durable(VoucherMqConstants.SECKILL_RETRY_QUEUE)
                .withArgument("x-message-ttl", 3000)
                .withArgument("x-dead-letter-exchange", VoucherMqConstants.SECKILL_ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", VoucherMqConstants.SECKILL_ORDER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillRetryBinding(Queue seckillRetryQueue, DirectExchange seckillRetryExchange) {
        return BindingBuilder.bind(seckillRetryQueue)
                .to(seckillRetryExchange)
                .with(VoucherMqConstants.SECKILL_RETRY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange seckillDeadExchange() {
        return new DirectExchange(VoucherMqConstants.SECKILL_DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillDeadQueue() {
        return QueueBuilder.durable(VoucherMqConstants.SECKILL_DEAD_QUEUE).build();
    }

    @Bean
    public Binding seckillDeadBinding(Queue seckillDeadQueue, DirectExchange seckillDeadExchange) {
        return BindingBuilder.bind(seckillDeadQueue)
                .to(seckillDeadExchange)
                .with(VoucherMqConstants.SECKILL_DEAD_ROUTING_KEY);
    }

    @Bean
    public DirectExchange canalCacheExchange() {
        return new DirectExchange(VoucherMqConstants.CANAL_CACHE_EXCHANGE, true, false);
    }

    @Bean
    public Queue canalCacheQueue() {
        return new Queue(VoucherMqConstants.CANAL_CACHE_QUEUE, true);
    }

    @Bean
    public Binding canalCacheBinding(Queue canalCacheQueue, DirectExchange canalCacheExchange) {
        return BindingBuilder.bind(canalCacheQueue)
                .to(canalCacheExchange)
                .with(VoucherMqConstants.CANAL_CACHE_ROUTING_KEY);
    }
}
