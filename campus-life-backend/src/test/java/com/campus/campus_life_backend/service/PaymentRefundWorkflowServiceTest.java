package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentRefundStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundReviewLogMapper;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRefundWorkflowServiceTest {

    @Mock
    private PaymentRefundOrderMapper paymentRefundOrderMapper;

    @Mock
    private PaymentRefundReviewLogMapper paymentRefundReviewLogMapper;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private VoucherOrderService voucherOrderService;

    @Mock
    private VoucherMapper voucherMapper;

    @Mock
    private MerchantMapper merchantMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MessageNotificationService messageNotificationService;

    @Mock
    private PaymentRefundDomainService paymentRefundDomainService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentRefundWorkflowService paymentRefundWorkflowService;

    @Test
    void shouldRejectDuplicateActiveRefundApplicationForSameOrder() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY001");
        paymentOrder.setBizOrderNo("ORDER001");

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setOrderNo("ORDER001");
        voucherOrder.setVoucherId(8L);

        PaymentRefundOrder latestRefund = new PaymentRefundOrder();
        latestRefund.setRefundNo("REF001");
        latestRefund.setBizOrderNo("ORDER001");
        latestRefund.setStatus(PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode());

        when(paymentRefundOrderMapper.findByRequestIdempotencyKey("refund-key")).thenReturn(null);
        when(paymentRefundOrderMapper.findLatestByBizOrderNo("ORDER001")).thenReturn(latestRefund);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> paymentRefundWorkflowService.submitVoucherRefundApplication(
                        paymentOrder,
                        voucherOrder,
                        1L,
                        new BigDecimal("19.90"),
                        "买错了",
                        "refund-key"
                )
        );

        assertEquals("该订单已有退款申请处理中", ex.getMessage());
        verify(paymentRefundOrderMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void merchantApprove_alipayChannel_callsPaymentServiceRefundOnly() {
        Merchant merchant = new Merchant();
        merchant.setId(9L);
        merchant.setStatus(1);

        PaymentRefundOrder refundOrder = new PaymentRefundOrder();
        refundOrder.setRefundNo("REF-ALI");
        refundOrder.setPaymentNo("PAY-ALI");
        refundOrder.setBizOrderNo("ORDER-ALI");
        refundOrder.setMerchantId(9L);
        refundOrder.setUserId(100L);
        refundOrder.setRefundAmount(new BigDecimal("20.00"));
        refundOrder.setStatus(PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode());

        PaymentRefundOrder processingRefund = new PaymentRefundOrder();
        processingRefund.setRefundNo("REF-ALI");
        processingRefund.setPaymentNo("PAY-ALI");
        processingRefund.setBizOrderNo("ORDER-ALI");
        processingRefund.setRefundAmount(new BigDecimal("20.00"));
        processingRefund.setReason("同意退款");
        processingRefund.setStatus(PaymentRefundStatus.PROCESSING.getCode());

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY-ALI");
        paymentOrder.setAmount(new BigDecimal("20.00"));
        paymentOrder.setChannel("alipay");

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setOrderNo("ORDER-ALI");
        voucherOrder.setStatus(1);
        voucherOrder.setUseTime(null);

        PaymentRefundResponse successResponse = new PaymentRefundResponse();
        successResponse.setRefundNo("REF-ALI");
        successResponse.setStatus(PaymentRefundStatus.SUCCESS.getCode());

        when(merchantMapper.findByUserId(50L)).thenReturn(merchant);
        when(paymentRefundOrderMapper.findByRefundNo("REF-ALI")).thenReturn(refundOrder, processingRefund);
        when(voucherOrderService.findByOrderNo("ORDER-ALI")).thenReturn(voucherOrder);
        when(paymentOrderMapper.findByPaymentNo("PAY-ALI")).thenReturn(paymentOrder);
        when(paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo("PAY-ALI")).thenReturn(BigDecimal.ZERO);
        when(paymentRefundOrderMapper.markMerchantApproved(
                eq("REF-ALI"),
                any(),
                any(),
                any(),
                eq(50L),
                eq(PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode())
        )).thenReturn(1);
        when(paymentService.refund("PAY-ALI", new BigDecimal("20.00"), "同意退款")).thenReturn(true);
        when(paymentRefundDomainService.markRefundSuccess(processingRefund, voucherOrder)).thenReturn(successResponse);

        paymentRefundWorkflowService.merchantApprove("REF-ALI", 50L, new BigDecimal("20.00"), "同意退款");

        verify(paymentService).refund("PAY-ALI", new BigDecimal("20.00"), "同意退款");
        verify(paymentRefundDomainService, never()).processWalletRefund(any(), any());
    }
}
