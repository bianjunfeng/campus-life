package com.campus.campus_life_backend.modules.voucher.config;

public final class VoucherMqConstants {

    private VoucherMqConstants() {
    }

    public static final String SECKILL_ORDER_EXCHANGE = "voucher.seckill.exchange";
    public static final String SECKILL_ORDER_QUEUE = "voucher.seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "voucher.seckill.order";
    public static final String SECKILL_RETRY_EXCHANGE = "voucher.seckill.retry.exchange";
    public static final String SECKILL_RETRY_QUEUE = "voucher.seckill.retry.queue";
    public static final String SECKILL_RETRY_ROUTING_KEY = "voucher.seckill.retry";
    public static final String SECKILL_DEAD_EXCHANGE = "voucher.seckill.dead.exchange";
    public static final String SECKILL_DEAD_QUEUE = "voucher.seckill.dead.queue";
    public static final String SECKILL_DEAD_ROUTING_KEY = "voucher.seckill.dead";

    public static final String CANAL_CACHE_EXCHANGE = "canal.cache.exchange";
    public static final String CANAL_CACHE_QUEUE = "canal.cache.sync.queue";
    public static final String CANAL_CACHE_ROUTING_KEY = "canal.cache.sync";
}
