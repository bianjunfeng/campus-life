package com.campus.campus_life_backend.modules.voucher.config;

public final class WelfareCacheKeys {

    private WelfareCacheKeys() {
    }

    public static final String WELFARE_HOME = "welfare:home:all";
    public static final String COUPON_LIST = "welfare:coupon:list";
    public static final String FLASH_SALE_LIST = "welfare:flash:list";

    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
    public static final String SECKILL_USER_SET_PREFIX = "seckill:users:";
    public static final String SECKILL_BEGIN_PREFIX = "seckill:begin:";
    public static final String SECKILL_END_PREFIX = "seckill:end:";
    public static final String SECKILL_RESULT_PREFIX = "seckill:result:";
    public static final String SECKILL_CONSUME_PREFIX = "seckill:consume:";
    public static final String SECKILL_ORDER_OWNER_PREFIX = "seckill:owner:";
    public static final String SECKILL_ORDER_ID_SEQ = "seckill:order:seq";

    public static String stockKey(Long voucherId) {
        return SECKILL_STOCK_PREFIX + voucherId;
    }

    public static String userSetKey(Long voucherId) {
        return SECKILL_USER_SET_PREFIX + voucherId;
    }

    public static String beginKey(Long voucherId) {
        return SECKILL_BEGIN_PREFIX + voucherId;
    }

    public static String endKey(Long voucherId) {
        return SECKILL_END_PREFIX + voucherId;
    }

    public static String resultKey(String orderNo) {
        return SECKILL_RESULT_PREFIX + orderNo;
    }

    public static String consumeKey(String orderNo) {
        return SECKILL_CONSUME_PREFIX + orderNo;
    }

    public static String orderOwnerKey(String orderNo) {
        return SECKILL_ORDER_OWNER_PREFIX + orderNo;
    }
}
