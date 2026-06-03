package com.campus.campus_life_backend.service;

import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import com.campus.campus_life_backend.modules.payment.enums.PaymentRefundStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundReviewLogMapper;
import com.campus.campus_life_backend.modules.payment.service.PaymentAutoRefundService;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundDomainService;
import com.campus.campus_life_backend.modules.payment.service.PaymentService;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAutoRefundServiceTest {

    @Mock
    private VoucherOrderService voucherOrderService;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private PaymentRefundOrderMapper paymentRefundOrderMapper;

    @Mock
    private PaymentRefundReviewLogMapper paymentRefundReviewLogMapper;

    @Mock
    private PaymentRefundDomainService paymentRefundDomainService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private MessageNotificationService messageNotificationService;

    @Mock
    private VoucherMapper voucherMapper;

    private PaymentAutoRefundService paymentAutoRefundService;

    @BeforeEach
    void setUp() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new NoopTransactionManager());
        paymentAutoRefundService = new PaymentAutoRefundService(
                voucherOrderService,
                paymentOrderMapper,
                paymentRefundOrderMapper,
                paymentRefundReviewLogMapper,
                paymentRefundDomainService,
                paymentService,
                messageNotificationService,
                voucherMapper,
                transactionTemplate
        );
    }

    @Test
    void shouldExpireAndRefundWalletOrderAutomatically() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(5);
        VoucherOrder candidate = buildOrder(1L, 1, 1, now.minusMinutes(1), now.minusMinutes(10));
        VoucherOrder expiredOrder = buildOrder(1L, 3, 1, now.minusMinutes(1), now.minusMinutes(5));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY001");
        paymentOrder.setAmount(new BigDecimal("29.90"));
        paymentOrder.setChannel("wallet");

        PaymentRefundResponse refundResponse = new PaymentRefundResponse();
        refundResponse.setRefundNo("REF001");
        refundResponse.setRefundAmount(new BigDecimal("29.90"));

        when(voucherOrderService.findExpiredPaidOrdersForAutoRefund(any(), eq(20))).thenReturn(List.of(candidate));
        when(voucherOrderService.findById(1L)).thenReturn(candidate, expiredOrder);
        when(voucherOrderService.markOrderExpired(eq(1L), anyString(), any())).thenReturn(true);
        when(paymentOrderMapper.findLatestByBizOrderNo(eq("VOUCHER"), eq(candidate.getOrderNo()))).thenReturn(paymentOrder);
        when(paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo("PAY001")).thenReturn(BigDecimal.ZERO);
        when(paymentRefundOrderMapper.findByRequestIdempotencyKey("AUTO_EXPIRE_REFUND:ORDER001")).thenReturn(null);
        when(paymentRefundDomainService.processWalletRefund(any(PaymentRefundOrder.class), eq(expiredOrder))).thenReturn(refundResponse);
        when(voucherMapper.findById(candidate.getVoucherId())).thenReturn(buildVoucher(9L));

        int refunded = paymentAutoRefundService.autoRefundExpiredVoucherOrders(20);

        assertEquals(1, refunded);
        ArgumentCaptor<PaymentRefundOrder> captor = ArgumentCaptor.forClass(PaymentRefundOrder.class);
        verify(paymentRefundOrderMapper).insert(captor.capture());
        PaymentRefundOrder inserted = captor.getValue();
        assertEquals("AUTO_EXPIRE_REFUND:ORDER001", inserted.getRequestIdempotencyKey());
        assertEquals(PaymentRefundStatus.PROCESSING.getCode(), inserted.getStatus());
        assertEquals(new BigDecimal("29.9"), inserted.getRefundAmount());
        verify(paymentRefundDomainService).processWalletRefund(any(PaymentRefundOrder.class), eq(expiredOrder));
        verify(messageNotificationService).createSystemNotification(anyString(), eq("AUTO_REFUND_SUCCESS"), eq(100L), eq(1L), anyString());
    }

    @Test
    void shouldExpireAndRefundAlipayOrderViaPaymentServiceOnly() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(5);
        VoucherOrder candidate = buildOrder(3L, 1, 1, now.minusMinutes(1), now.minusMinutes(10));
        VoucherOrder expiredOrder = buildOrder(3L, 3, 1, now.minusMinutes(1), now.minusMinutes(5));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY003");
        paymentOrder.setAmount(new BigDecimal("39.90"));
        paymentOrder.setChannel("alipay");

        PaymentRefundResponse refundResponse = new PaymentRefundResponse();
        refundResponse.setRefundNo("REF003");
        refundResponse.setRefundAmount(new BigDecimal("39.90"));

        when(voucherOrderService.findExpiredPaidOrdersForAutoRefund(any(), eq(20))).thenReturn(List.of(candidate));
        when(voucherOrderService.findById(3L)).thenReturn(candidate, expiredOrder);
        when(voucherOrderService.markOrderExpired(eq(3L), anyString(), any())).thenReturn(true);
        when(paymentOrderMapper.findLatestByBizOrderNo(eq("VOUCHER"), eq(candidate.getOrderNo()))).thenReturn(paymentOrder);
        when(paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo("PAY003")).thenReturn(BigDecimal.ZERO);
        when(paymentRefundOrderMapper.findByRequestIdempotencyKey("AUTO_EXPIRE_REFUND:ORDER003")).thenReturn(null);
        when(paymentService.refund(eq("PAY003"), eq(new BigDecimal("39.9")), eq("券到期自动退款"))).thenReturn(true);
        when(paymentRefundDomainService.markRefundSuccess(any(PaymentRefundOrder.class), eq(expiredOrder)))
                .thenReturn(refundResponse);
        when(voucherMapper.findById(candidate.getVoucherId())).thenReturn(buildVoucher(9L));

        int refunded = paymentAutoRefundService.autoRefundExpiredVoucherOrders(20);

        assertEquals(1, refunded);
        verify(paymentService).refund("PAY003", new BigDecimal("39.9"), "券到期自动退款");
        verify(paymentRefundDomainService, never()).processWalletRefund(any(), any());
    }

    @Test
    void shouldRetryFailedAutoRefundRecord() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(5);
        VoucherOrder expiredOrder = buildOrder(2L, 3, 1, now.minusMinutes(1), now.minusMinutes(5));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo("PAY002");
        paymentOrder.setAmount(new BigDecimal("19.90"));
        paymentOrder.setChannel("wallet");

        PaymentRefundOrder failedRefund = new PaymentRefundOrder();
        failedRefund.setRefundNo("REF_FAILED");
        failedRefund.setStatus(PaymentRefundStatus.FAILED.getCode());
        failedRefund.setRequestIdempotencyKey("AUTO_EXPIRE_REFUND:ORDER002");

        PaymentRefundOrder processingRefund = new PaymentRefundOrder();
        processingRefund.setRefundNo("REF_FAILED");
        processingRefund.setStatus(PaymentRefundStatus.PROCESSING.getCode());
        processingRefund.setRefundAmount(new BigDecimal("19.90"));
        processingRefund.setReason("券到期自动退款");

        PaymentRefundResponse refundResponse = new PaymentRefundResponse();
        refundResponse.setRefundNo("REF_FAILED");
        refundResponse.setRefundAmount(new BigDecimal("19.90"));

        when(voucherOrderService.findExpiredPaidOrdersForAutoRefund(any(), eq(10))).thenReturn(List.of(expiredOrder));
        when(voucherOrderService.findById(2L)).thenReturn(expiredOrder);
        when(paymentOrderMapper.findLatestByBizOrderNo(eq("VOUCHER"), eq(expiredOrder.getOrderNo()))).thenReturn(paymentOrder);
        when(paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo("PAY002")).thenReturn(BigDecimal.ZERO);
        when(paymentRefundOrderMapper.findByRequestIdempotencyKey("AUTO_EXPIRE_REFUND:ORDER002")).thenReturn(failedRefund);
        when(paymentRefundOrderMapper.markProcessingFromFailed("REF_FAILED", PaymentRefundStatus.FAILED.getCode())).thenReturn(1);
        when(paymentRefundOrderMapper.findByRefundNo("REF_FAILED")).thenReturn(processingRefund);
        when(paymentRefundDomainService.processWalletRefund(processingRefund, expiredOrder)).thenReturn(refundResponse);

        int refunded = paymentAutoRefundService.autoRefundExpiredVoucherOrders(10);

        assertEquals(1, refunded);
        verify(paymentRefundOrderMapper).markProcessingFromFailed("REF_FAILED", PaymentRefundStatus.FAILED.getCode());
        verify(paymentRefundDomainService).processWalletRefund(processingRefund, expiredOrder);
        verify(paymentRefundReviewLogMapper, times(2)).insert(any());
    }

    private VoucherOrder buildOrder(Long id, Integer status, Integer paymentStatus, LocalDateTime useDeadline, LocalDateTime updateTime) {
        VoucherOrder order = new VoucherOrder();
        order.setId(id);
        order.setOrderNo("ORDER00" + id);
        order.setUserId(99L + id);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setVoucherId(700L + id);
        order.setUseDeadline(useDeadline);
        order.setUpdateTime(updateTime);
        return order;
    }

    private Voucher buildVoucher(Long merchantId) {
        Voucher voucher = new Voucher();
        voucher.setMerchantId(merchantId);
        return voucher;
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
