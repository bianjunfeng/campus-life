package com.campus.campus_life_backend.modules.order.event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class AbstractVoucherOrderEvent {

    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderNo;
    private String paymentNo;
    private Long userId;
    private String paymentMethod;
    private String reason;
    private String requestIdempotencyKey;
    private LocalDateTime payTime;
    private LocalDateTime eventTime;
    private String source;

    protected AbstractVoucherOrderEvent(String eventType) {
        this.eventType = eventType;
    }

    protected AbstractVoucherOrderEvent() {
    }
}
