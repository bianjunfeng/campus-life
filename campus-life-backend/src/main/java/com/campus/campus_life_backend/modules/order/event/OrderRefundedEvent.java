package com.campus.campus_life_backend.modules.order.event;

public class OrderRefundedEvent extends AbstractVoucherOrderEvent {

    public static final String EVENT_TYPE = "ORDER_REFUNDED";

    public OrderRefundedEvent() {
        super(EVENT_TYPE);
    }
}
