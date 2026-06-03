package com.campus.campus_life_backend.modules.order.event;

public class OrderCancelledEvent extends AbstractVoucherOrderEvent {

    public static final String EVENT_TYPE = "ORDER_CANCELLED";

    public OrderCancelledEvent() {
        super(EVENT_TYPE);
    }
}
