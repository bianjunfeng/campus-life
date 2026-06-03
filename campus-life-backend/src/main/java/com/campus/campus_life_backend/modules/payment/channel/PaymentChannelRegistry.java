package com.campus.campus_life_backend.modules.payment.channel;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentChannelRegistry {

    private final Map<String, PaymentChannelHandler> handlersByCode;

    public PaymentChannelRegistry(List<PaymentChannelHandler> handlers) {
        Map<String, PaymentChannelHandler> map = new LinkedHashMap<>();
        for (PaymentChannelHandler handler : handlers) {
            String code = handler.channelCode();
            if (map.containsKey(code)) {
                throw new IllegalStateException("重复的支付渠道编码: " + code);
            }
            map.put(code, handler);
        }
        this.handlersByCode = Map.copyOf(map);
    }

    public PaymentChannelHandler require(String channelCode) {
        PaymentChannelHandler handler = handlersByCode.get(channelCode);
        if (handler == null) {
            throw new IllegalArgumentException("未知的支付渠道: " + channelCode);
        }
        return handler;
    }
}
