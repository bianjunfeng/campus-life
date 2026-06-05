package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.payment.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
            if (!isKnownPaymentMethod(code)) {
                throw new IllegalStateException("未识别的支付渠道编码: " + code);
            }
            map.put(code, handler);
        }

        List<String> missing = new ArrayList<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            if (!map.containsKey(method.getCode())) {
                missing.add(method.getCode());
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("缺少支付渠道 Handler: " + String.join(", ", missing));
        }

        this.handlersByCode = Map.copyOf(map);
    }

    private static boolean isKnownPaymentMethod(String code) {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public PaymentChannelHandler require(String channelCode) {
        PaymentChannelHandler handler = handlersByCode.get(channelCode);
        if (handler == null) {
            throw new IllegalArgumentException("未知的支付渠道: " + channelCode);
        }
        return handler;
    }
}
