package com.campus.campus_life_backend.modules.order.event;

public class OrderPaymentFailedEvent extends AbstractVoucherOrderEvent {

    public static final String EVENT_TYPE = "ORDER_PAYMENT_FAILED";

    public OrderPaymentFailedEvent() {
        super(EVENT_TYPE);
    }
}
