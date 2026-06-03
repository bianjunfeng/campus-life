package com.campus.campus_life_backend.modules.voucher.service;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.campus.campus_life_backend.modules.voucher.config.VoucherMqConstants;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class SeckillOpsService {

    private final RedisConnectionFactory redisConnectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final VoucherSeckillService voucherSeckillService;
    private final SeckillMonitorMetricsService seckillMonitorMetricsService;

    public SeckillOpsService(
            RedisConnectionFactory redisConnectionFactory,
            AmqpAdmin amqpAdmin,
            VoucherSeckillService voucherSeckillService,
            SeckillMonitorMetricsService seckillMonitorMetricsService
    ) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.amqpAdmin = amqpAdmin;
        this.voucherSeckillService = voucherSeckillService;
        this.seckillMonitorMetricsService = seckillMonitorMetricsService;
    }

    public Map<String, Object> getHealth(Long voucherId) {
        Map<String, Object> result = new HashMap<>();

        result.put("redis", checkRedis());
        result.put("rabbitmq", checkRabbitMq());
        result.put("sentinel", checkSentinel());
        result.put("metrics", seckillMonitorMetricsService.snapshot());
        result.put("checkedAt", Instant.now().toString());

        if (voucherId != null) {
            result.put("voucherMonitor", voucherSeckillService.getSeckillMonitor(voucherId));
        }
        result.put("alerts", buildAlerts(result));
        result.put("overallStatus", calcOverallStatus(result));

        return result;
    }

    public void resetMetrics() {
        seckillMonitorMetricsService.reset();
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> redis = new HashMap<>();
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            redis.put("status", "OK");
            redis.put("ping", pong);
        } catch (Exception e) {
            redis.put("status", "DOWN");
            redis.put("message", e.getMessage());
        }
        return redis;
    }

    private Map<String, Object> checkRabbitMq() {
        Map<String, Object> rabbit = new HashMap<>();
        try {
            Properties seckillQueue = amqpAdmin.getQueueProperties(VoucherMqConstants.SECKILL_ORDER_QUEUE);
            Properties seckillRetryQueue = amqpAdmin.getQueueProperties(VoucherMqConstants.SECKILL_RETRY_QUEUE);
            Properties seckillDeadQueue = amqpAdmin.getQueueProperties(VoucherMqConstants.SECKILL_DEAD_QUEUE);
            Properties canalQueue = amqpAdmin.getQueueProperties(VoucherMqConstants.CANAL_CACHE_QUEUE);
            rabbit.put("status", "OK");
            rabbit.put("seckillQueue", queueMetric(seckillQueue));
            rabbit.put("seckillRetryQueue", queueMetric(seckillRetryQueue));
            rabbit.put("seckillDeadQueue", queueMetric(seckillDeadQueue));
            rabbit.put("canalQueue", queueMetric(canalQueue));
        } catch (Exception e) {
            rabbit.put("status", "DOWN");
            rabbit.put("message", e.getMessage());
        }
        return rabbit;
    }

    private Map<String, Object> queueMetric(Properties queueProps) {
        Map<String, Object> metric = new HashMap<>();
        if (queueProps == null) {
            metric.put("exists", false);
            return metric;
        }
        metric.put("exists", true);
        metric.put("messageCount", queueProps.get("QUEUE_MESSAGE_COUNT"));
        metric.put("consumerCount", queueProps.get("QUEUE_CONSUMER_COUNT"));
        return metric;
    }

    private Map<String, Object> checkSentinel() {
        Map<String, Object> sentinel = new HashMap<>();
        sentinel.put("status", "OK");
        sentinel.put("flowRuleCount", FlowRuleManager.getRules().size());
        return sentinel;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildAlerts(Map<String, Object> health) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        Map<String, Object> redis = (Map<String, Object>) health.get("redis");
        Map<String, Object> rabbitmq = (Map<String, Object>) health.get("rabbitmq");
        Map<String, Object> sentinel = (Map<String, Object>) health.get("sentinel");
        Map<String, Object> metrics = (Map<String, Object>) health.get("metrics");

        if (redis != null && !"OK".equals(redis.get("status"))) {
            alerts.add(alert("CRITICAL", "Redis不可用"));
        }
        if (rabbitmq != null && !"OK".equals(rabbitmq.get("status"))) {
            alerts.add(alert("CRITICAL", "RabbitMQ不可用"));
        }
        if (sentinel != null && toLong(sentinel.get("flowRuleCount")) <= 0) {
            alerts.add(alert("WARN", "Sentinel流控规则数为0"));
        }

        if (rabbitmq != null) {
            Map<String, Object> seckillQueue = (Map<String, Object>) rabbitmq.get("seckillQueue");
            long queueCount = toLong(seckillQueue == null ? null : seckillQueue.get("messageCount"));
            if (queueCount > 1000) {
                alerts.add(alert("WARN", "秒杀主队列堆积超过1000"));
            }
            Map<String, Object> deadQueue = (Map<String, Object>) rabbitmq.get("seckillDeadQueue");
            long deadCount = toLong(deadQueue == null ? null : deadQueue.get("messageCount"));
            if (deadCount > 0) {
                alerts.add(alert("WARN", "存在死信消息，请尽快排查"));
            }
        }

        if (metrics != null) {
            double processSuccessRate = toDouble(metrics.get("processSuccessRate"));
            if (processSuccessRate > 0 && processSuccessRate < 95) {
                alerts.add(alert("WARN", "秒杀消费成功率低于95%"));
            }
            long mqPublishFail = toLong(metrics.get("mqPublishFailCount"));
            if (mqPublishFail > 0) {
                alerts.add(alert("WARN", "存在MQ投递失败记录"));
            }
            double avgQueueDelayMs = toDouble(metrics.get("avgQueueDelayMs"));
            if (avgQueueDelayMs > 3000) {
                alerts.add(alert("WARN", "平均排队延迟超过3秒"));
            }
        }
        return alerts;
    }

    @SuppressWarnings("unchecked")
    private String calcOverallStatus(Map<String, Object> health) {
        Map<String, Object> redis = (Map<String, Object>) health.get("redis");
        Map<String, Object> rabbitmq = (Map<String, Object>) health.get("rabbitmq");
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) health.get("alerts");
        if (redis != null && !"OK".equals(redis.get("status"))) {
            return "CRITICAL";
        }
        if (rabbitmq != null && !"OK".equals(rabbitmq.get("status"))) {
            return "CRITICAL";
        }
        if (alerts != null && !alerts.isEmpty()) {
            return "WARN";
        }
        return "OK";
    }

    private Map<String, Object> alert(String level, String message) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("level", level);
        alert.put("message", message);
        return alert;
    }

    private long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value == null) {
            return 0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0D;
        }
    }
}
