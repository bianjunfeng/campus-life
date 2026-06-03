package com.campus.campus_life_backend.modules.voucher.listener;

import com.campus.campus_life_backend.modules.voucher.config.VoucherMqConstants;
import com.campus.campus_life_backend.modules.voucher.dto.SeckillOrderMessage;
import com.campus.campus_life_backend.modules.voucher.service.SeckillMonitorMetricsService;
import com.campus.campus_life_backend.modules.voucher.service.SeckillOrderProcessService;
import com.campus.campus_life_backend.modules.voucher.service.VoucherSeckillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "voucher.seckill.consumer.enabled", havingValue = "true")
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final ObjectMapper objectMapper;
    private final SeckillOrderProcessService seckillOrderProcessService;
    private final VoucherSeckillService voucherSeckillService;
    private final SeckillMonitorMetricsService seckillMonitorMetricsService;
    private final RabbitTemplate rabbitTemplate;

    public SeckillOrderConsumer(
            ObjectMapper objectMapper,
            SeckillOrderProcessService seckillOrderProcessService,
            VoucherSeckillService voucherSeckillService,
            SeckillMonitorMetricsService seckillMonitorMetricsService,
            RabbitTemplate rabbitTemplate
    ) {
        this.objectMapper = objectMapper;
        this.seckillOrderProcessService = seckillOrderProcessService;
        this.voucherSeckillService = voucherSeckillService;
        this.seckillMonitorMetricsService = seckillMonitorMetricsService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "voucher.seckill.order.queue")
    public void consume(String payload, Message rawMessage) {
        SeckillOrderMessage message;
        try {
            message = objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (Exception e) {
            log.error("秒杀消息反序列化失败: {}", payload, e);
            return;
        }

        try {
            if (!voucherSeckillService.tryMarkConsuming(message.getOrderNo())) {
                return;
            }
            long start = System.currentTimeMillis();
            SeckillOrderProcessService.ProcessStatus status = seckillOrderProcessService.processOrder(message);
            if (status == SeckillOrderProcessService.ProcessStatus.SUCCESS) {
                seckillMonitorMetricsService.recordProcessSuccess(
                        message,
                        System.currentTimeMillis() - start,
                        "consumer"
                );
                voucherSeckillService.markResultSuccess(message.getOrderNo());
            }
        } catch (SeckillOrderProcessService.SeckillNonRetryableException e) {
            seckillMonitorMetricsService.recordProcessFail(message, e.getMessage(), "consumer_non_retryable");
            voucherSeckillService.markResultFail(message.getOrderNo(), e.getMessage());
            voucherSeckillService.rollbackReservation(message.getVoucherId(), message.getUserId());
        } catch (Exception e) {
            int retryCount = getRetryCount(rawMessage);
            log.error("消费秒杀订单失败 payload={}, retryCount={}", payload, retryCount, e);
            if (retryCount >= MAX_RETRY_COUNT) {
                seckillMonitorMetricsService.recordDeadLetter(message);
                seckillMonitorMetricsService.recordProcessFail(message, e.getMessage(), "consumer_dead_letter");
                voucherSeckillService.markResultFail(message.getOrderNo(), "系统繁忙，请稍后重试");
                voucherSeckillService.rollbackReservation(message.getVoucherId(), message.getUserId());
                rabbitTemplate.convertAndSend(
                        VoucherMqConstants.SECKILL_DEAD_EXCHANGE,
                        VoucherMqConstants.SECKILL_DEAD_ROUTING_KEY,
                        payload
                );
                return;
            }
            seckillMonitorMetricsService.recordRetry(message);
            throw new AmqpRejectAndDontRequeueException("秒杀消费失败，进入重试队列");
        }
    }

    @SuppressWarnings("unchecked")
    private int getRetryCount(Message rawMessage) {
        Object xDeath = rawMessage.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeath instanceof List<?> deaths) || deaths.isEmpty()) {
            return 0;
        }
        Object first = deaths.get(0);
        if (!(first instanceof Map<?, ?> deathMap)) {
            return 0;
        }
        Object countObj = deathMap.get("count");
        if (countObj instanceof Long l) {
            return l.intValue();
        }
        if (countObj instanceof Integer i) {
            return i;
        }
        return 0;
    }
}
