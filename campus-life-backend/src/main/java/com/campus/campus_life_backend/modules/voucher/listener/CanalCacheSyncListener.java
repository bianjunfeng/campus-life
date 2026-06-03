package com.campus.campus_life_backend.modules.voucher.listener;

import com.campus.campus_life_backend.modules.voucher.dto.CanalMessage;
import com.campus.campus_life_backend.modules.voucher.service.WelfareQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CanalCacheSyncListener {

    private static final Logger log = LoggerFactory.getLogger(CanalCacheSyncListener.class);
    private static final Set<String> RELATED_TABLES = Set.of("voucher", "seckill_voucher", "voucher_order");

    private final ObjectMapper objectMapper;
    private final WelfareQueryService welfareQueryService;

    public CanalCacheSyncListener(ObjectMapper objectMapper, WelfareQueryService welfareQueryService) {
        this.objectMapper = objectMapper;
        this.welfareQueryService = welfareQueryService;
    }

    @RabbitListener(queues = "canal.cache.sync.queue")
    public void onMessage(String payload) {
        try {
            CanalMessage message = objectMapper.readValue(payload, CanalMessage.class);
            if (message.getTable() != null && RELATED_TABLES.contains(message.getTable())) {
                welfareQueryService.evictWelfareCache();
            }
        } catch (Exception e) {
            log.warn("Canal缓存同步消息解析失败: {}", payload, e);
        }
    }
}
