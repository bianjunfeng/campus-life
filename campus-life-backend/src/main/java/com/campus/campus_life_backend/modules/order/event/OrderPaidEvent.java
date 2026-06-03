package com.campus.campus_life_backend.modules.order.event;

public class OrderPaidEvent extends AbstractVoucherOrderEvent {

    public static final String EVENT_TYPE = "ORDER_PAID";

    public OrderPaidEvent() {
        super(EVENT_TYPE);
    }
}
