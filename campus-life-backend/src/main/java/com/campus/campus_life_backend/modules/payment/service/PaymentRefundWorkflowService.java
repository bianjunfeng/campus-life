package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.payment.dto.PaymentRefundResponse;
import com.campus.campus_life_backend.modules.payment.entity.PaymentOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundOrder;
import com.campus.campus_life_backend.modules.payment.entity.PaymentRefundReviewLog;
import com.campus.campus_life_backend.modules.payment.enums.PaymentBizType;
import com.campus.campus_life_backend.modules.payment.enums.PaymentRefundStatus;
import com.campus.campus_life_backend.modules.order.service.VoucherOrderService;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundReviewLogMapper;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.order.entity.VoucherOrder;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentRefundWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRefundWorkflowService.class);
    private static final DateTimeFormatter REFUND_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String REVIEWER_MERCHANT = "MERCHANT";
    private static final String REVIEWER_ADMIN = "ADMIN";
    private static final String REVIEWER_SYSTEM = "SYSTEM";

    private final PaymentRefundOrderMapper paymentRefundOrderMapper;
    private final PaymentRefundReviewLogMapper paymentRefundReviewLogMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final VoucherOrderService voucherOrderService;
    private final VoucherMapper voucherMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final MessageNotificationService messageNotificationService;
    private final PaymentRefundDomainService paymentRefundDomainService;
    private final PaymentService paymentService;

    public PaymentRefundWorkflowService(
            PaymentRefundOrderMapper paymentRefundOrderMapper,
            PaymentRefundReviewLogMapper paymentRefundReviewLogMapper,
            PaymentOrderMapper paymentOrderMapper,
            VoucherOrderService voucherOrderService,
            VoucherMapper voucherMapper,
            MerchantMapper merchantMapper,
            UserMapper userMapper,
            MessageNotificationService messageNotificationService,
            PaymentRefundDomainService paymentRefundDomainService,
            PaymentService paymentService
    ) {
        this.paymentRefundOrderMapper = paymentRefundOrderMapper;
        this.paymentRefundReviewLogMapper = paymentRefundReviewLogMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.voucherOrderService = voucherOrderService;
        this.voucherMapper = voucherMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
        this.messageNotificationService = messageNotificationService;
        this.paymentRefundDomainService = paymentRefundDomainService;
        this.paymentService = paymentService;
    }

    @Transactional
    @RequirePermission(anyOf = {"payment:refund:apply:self"})
    public PaymentRefundResponse submitVoucherRefundApplication(
            PaymentOrder paymentOrder,
            VoucherOrder voucherOrder,
            Long userId,
            BigDecimal refundAmount,
            String reason,
            String requestIdempotencyKey
    ) {
        PaymentRefundOrder existing = paymentRefundOrderMapper.findByRequestIdempotencyKey(requestIdempotencyKey);
        if (existing != null) {
            if (isFinalStatus(existing.getStatus())) {
                requestIdempotencyKey = requestIdempotencyKey + ":retry:" + System.currentTimeMillis();
            } else {
                return buildResponse(existing);
            }
        }

        PaymentRefundOrder latestRefund = paymentRefundOrderMapper.findLatestByBizOrderNo(paymentOrder.getBizOrderNo());
        if (latestRefund != null && !isFinalStatus(latestRefund.getStatus())) {
            throw new IllegalArgumentException("该订单已有退款申请处理中");
        }

        Voucher voucher = requireVoucher(voucherOrder.getVoucherId());
        validateRefundableAmount(paymentOrder, null, normalizeAmount(refundAmount));

        PaymentRefundOrder refundOrder = new PaymentRefundOrder();
        refundOrder.setRefundNo(generateRefundNo());
        refundOrder.setPaymentNo(paymentOrder.getPaymentNo());
        refundOrder.setBizType(PaymentBizType.VOUCHER.getCode());
        refundOrder.setBizOrderNo(paymentOrder.getBizOrderNo());
        refundOrder.setUserId(userId);
        refundOrder.setMerchantId(voucher.getMerchantId());
        refundOrder.setChannel(paymentOrder.getChannel());
        refundOrder.setRefundAmount(normalizeAmount(refundAmount));
        refundOrder.setRequestedAmount(normalizeAmount(refundAmount));
        refundOrder.setReason(normalizeReason(reason));
        refundOrder.setStatus(PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode());
        refundOrder.setCurrentReviewerRole(REVIEWER_MERCHANT);
        refundOrder.setReviewDeadline(LocalDateTime.now().plusHours(24));
        refundOrder.setRequestIdempotencyKey(requestIdempotencyKey);
        paymentRefundOrderMapper.insert(refundOrder);
        insertReviewLog(refundOrder.getRefundNo(), "USER", userId, "SUBMIT", null, refundOrder.getStatus(), refundOrder.getReason());
        Merchant merchant = merchantMapper.findById(voucher.getMerchantId());
        if (merchant != null && merchant.getUserId() != null) {
            notifyUser(
                    merchant.getUserId(),
                    userId,
                    "MERCHANT_REFUND_TODO",
                    String.format("有新的退款申请待处理：订单%s，金额¥%s", refundOrder.getBizOrderNo(), refundOrder.getRequestedAmount())
            );
        }
        return buildResponse(refundOrder);
    }

    public List<Map<String, Object>> listUserRefunds(Long userId) {
        return paymentRefundOrderMapper.findUserRefundViews(userId);
    }

    public Map<String, Object> getUserRefundDetail(Long userId, String refundNo) {
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        if (!refundOrder.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权查看该退款单");
        }
        Map<String, Object> detail = paymentRefundOrderMapper.findUserRefundViews(userId).stream()
                .filter(item -> refundNo.equals(String.valueOf(item.get("refundNo"))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        return attachReviewLogs(detail, refundNo);
    }

    @RequirePermission(anyOf = {"merchant:refund:review:self"})
    public List<Map<String, Object>> listMerchantRefunds(Long merchantUserId, String status) {
        Merchant merchant = requireVerifiedMerchant(merchantUserId);
        return paymentRefundOrderMapper.findMerchantRefundViews(merchant.getId(), normalizeStatusFilter(status));
    }

    @RequirePermission(anyOf = {"merchant:refund:review:self"})
    public Map<String, Object> getMerchantRefundDetail(Long merchantUserId, String refundNo) {
        Merchant merchant = requireVerifiedMerchant(merchantUserId);
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        if (!merchant.getId().equals(refundOrder.getMerchantId())) {
            throw new IllegalArgumentException("无权查看该退款单");
        }
        Map<String, Object> detail = paymentRefundOrderMapper.findMerchantRefundViews(merchant.getId(), null).stream()
                .filter(item -> refundNo.equals(String.valueOf(item.get("refundNo"))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        return attachReviewLogs(detail, refundNo);
    }

    @RequirePermission(anyOf = {"admin:refund:review"})
    public List<Map<String, Object>> listAdminRefunds(String status) {
        return paymentRefundOrderMapper.findAdminRefundViews(normalizeStatusFilter(status));
    }

    public long countPendingAdminRefunds() {
        return paymentRefundOrderMapper.countByStatus(PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode());
    }

    @RequirePermission(anyOf = {"admin:refund:review"})
    public Map<String, Object> getAdminRefundDetail(String refundNo) {
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        Map<String, Object> detail = paymentRefundOrderMapper.findAdminRefundViews(null).stream()
                .filter(item -> refundNo.equals(String.valueOf(item.get("refundNo"))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        return attachReviewLogs(detail, refundNo);
    }

    @Transactional
    @RequirePermission(anyOf = {"merchant:refund:review:self"})
    public PaymentRefundResponse merchantApprove(String refundNo, Long merchantUserId, BigDecimal approvedAmount, String reason) {
        Merchant merchant = requireVerifiedMerchant(merchantUserId);
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        if (!merchant.getId().equals(refundOrder.getMerchantId())) {
            throw new IllegalArgumentException("无权处理该退款单");
        }
        if (!PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode().equalsIgnoreCase(refundOrder.getStatus())) {
            throw new IllegalArgumentException("当前退款单不在商家审核阶段");
        }

        VoucherOrder voucherOrder = requireVoucherOrder(refundOrder.getBizOrderNo());
        ensureOrderRefundable(voucherOrder);
        PaymentOrder paymentOrder = requirePaymentOrder(refundOrder.getPaymentNo());
        BigDecimal targetAmount = resolveApprovedAmount(refundOrder, paymentOrder, approvedAmount);
        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = paymentRefundOrderMapper.markMerchantApproved(
                refundNo,
                targetAmount,
                normalizeReason(reason),
                reviewTime,
                merchantUserId,
                PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode()
        );
        if (updated <= 0) {
            throw new IllegalStateException("退款单状态已变更，请刷新后重试");
        }
        insertReviewLog(refundNo, REVIEWER_MERCHANT, merchantUserId, "MERCHANT_APPROVE",
                PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode(), PaymentRefundStatus.PROCESSING.getCode(), reason);
        PaymentRefundOrder latest = requireRefundOrder(refundNo);
        notifyUser(
                latest.getUserId(),
                merchantUserId,
                "REFUND_APPROVED",
                String.format("你的退款申请已通过，订单%s 正在退款处理中", latest.getBizOrderNo())
        );
        return executeApprovedRefund(latest, voucherOrder, paymentOrder);
    }

    @Transactional
    @RequirePermission(anyOf = {"merchant:refund:review:self"})
    public PaymentRefundResponse merchantRejectToAdmin(String refundNo, Long merchantUserId, String reason) {
        Merchant merchant = requireVerifiedMerchant(merchantUserId);
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        if (!merchant.getId().equals(refundOrder.getMerchantId())) {
            throw new IllegalArgumentException("无权处理该退款单");
        }
        if (!PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode().equalsIgnoreCase(refundOrder.getStatus())) {
            throw new IllegalArgumentException("当前退款单不在商家审核阶段");
        }

        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = paymentRefundOrderMapper.escalateToAdminReview(
                refundNo,
                normalizeReason(reason),
                reviewTime,
                merchantUserId,
                reviewTime.plusHours(24),
                PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode()
        );
        if (updated <= 0) {
            throw new IllegalStateException("退款单状态已变更，请刷新后重试");
        }
        insertReviewLog(refundNo, REVIEWER_MERCHANT, merchantUserId, "MERCHANT_REJECT",
                PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode(), PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode(), reason);
        PaymentRefundResponse response = buildResponse(requireRefundOrder(refundNo));
        notifyUser(
                refundOrder.getUserId(),
                merchantUserId,
                "REFUND_ESCALATED",
                String.format("你的退款申请已转交平台处理：订单%s", refundOrder.getBizOrderNo())
        );
        notifyAdmins(
                merchantUserId,
                "ADMIN_REFUND_TODO",
                String.format("有退款申请待平台处理：订单%s，退款单%s", refundOrder.getBizOrderNo(), refundNo)
        );
        return response;
    }

    @Transactional
    @RequirePermission(anyOf = {"admin:refund:review"})
    public PaymentRefundResponse adminApprove(String refundNo, Long adminUserId, BigDecimal approvedAmount, String reason) {
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        if (!PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode().equalsIgnoreCase(refundOrder.getStatus())) {
            throw new IllegalArgumentException("当前退款单不在管理员审核阶段");
        }

        VoucherOrder voucherOrder = requireVoucherOrder(refundOrder.getBizOrderNo());
        ensureOrderRefundable(voucherOrder);
        PaymentOrder paymentOrder = requirePaymentOrder(refundOrder.getPaymentNo());
        BigDecimal targetAmount = resolveApprovedAmount(refundOrder, paymentOrder, approvedAmount);
        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = paymentRefundOrderMapper.markAdminApproved(
                refundNo,
                targetAmount,
                normalizeReason(reason),
                reviewTime,
                adminUserId,
                PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode()
        );
        if (updated <= 0) {
            throw new IllegalStateException("退款单状态已变更，请刷新后重试");
        }
        insertReviewLog(refundNo, REVIEWER_ADMIN, adminUserId, "ADMIN_APPROVE",
                PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode(), PaymentRefundStatus.PROCESSING.getCode(), reason);
        PaymentRefundOrder latest = requireRefundOrder(refundNo);
        notifyUser(
                latest.getUserId(),
                adminUserId,
                "REFUND_APPROVED",
                String.format("平台已通过你的退款申请，订单%s 正在退款处理中", latest.getBizOrderNo())
        );
        return executeApprovedRefund(latest, voucherOrder, paymentOrder);
    }

    @Transactional
    @RequirePermission(anyOf = {"admin:refund:review"})
    public PaymentRefundResponse adminReject(String refundNo, Long adminUserId, String reason) {
        PaymentRefundOrder refundOrder = requireRefundOrder(refundNo);
        if (!PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode().equalsIgnoreCase(refundOrder.getStatus())) {
            throw new IllegalArgumentException("当前退款单不在管理员审核阶段");
        }

        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = paymentRefundOrderMapper.markAdminRejected(
                refundNo,
                normalizeReason(reason),
                reviewTime,
                adminUserId,
                PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode()
        );
        if (updated <= 0) {
            throw new IllegalStateException("退款单状态已变更，请刷新后重试");
        }
        insertReviewLog(refundNo, REVIEWER_ADMIN, adminUserId, "ADMIN_REJECT",
                PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode(), PaymentRefundStatus.REJECTED.getCode(), reason);
        PaymentRefundResponse response = buildResponse(requireRefundOrder(refundNo));
        notifyUser(
                refundOrder.getUserId(),
                adminUserId,
                "REFUND_REJECTED",
                String.format("平台已驳回你的退款申请：订单%s", refundOrder.getBizOrderNo())
        );
        return response;
    }

    @Transactional
    public int autoEscalateTimeoutMerchantReviews(int limit) {
        int safeLimit = limit <= 0 ? 100 : limit;
        LocalDateTime now = LocalDateTime.now();
        List<PaymentRefundOrder> overdueOrders = paymentRefundOrderMapper.findOverdueMerchantReviewOrders(now, safeLimit);
        int escalated = 0;
        for (PaymentRefundOrder order : overdueOrders) {
            int updated = paymentRefundOrderMapper.escalateTimeoutToAdminReview(
                    order.getRefundNo(),
                    "商家超时未处理，系统自动升级平台审核",
                    now,
                    now.plusHours(24),
                    PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode()
            );
            if (updated > 0) {
                insertReviewLog(order.getRefundNo(), REVIEWER_SYSTEM, null, "SYSTEM_ESCALATE",
                        PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode(), PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode(),
                        "商家超时未处理，系统自动升级平台审核");
                notifyAdmins(
                        order.getUserId() == null ? 1L : order.getUserId(),
                        "ADMIN_REFUND_TODO",
                        String.format("退款申请超时自动升级平台处理：订单%s，退款单%s", order.getBizOrderNo(), order.getRefundNo())
                );
                escalated++;
            }
        }
        return escalated;
    }

    private PaymentRefundResponse executeApprovedRefund(PaymentRefundOrder refundOrder, VoucherOrder voucherOrder, PaymentOrder paymentOrder) {
        try {
            if ("wallet".equalsIgnoreCase(paymentOrder.getChannel())) {
                return paymentRefundDomainService.processWalletRefund(refundOrder, voucherOrder);
            }
            boolean success = paymentService.refund(
                    paymentOrder.getPaymentNo(),
                    refundOrder.getRefundAmount(),
                    refundOrder.getReason()
            );
            if (!success) {
                paymentRefundDomainService.markRefundFailed(refundOrder);
                insertReviewLog(refundOrder.getRefundNo(), REVIEWER_SYSTEM, null, "SYSTEM_FAILED",
                        PaymentRefundStatus.PROCESSING.getCode(), PaymentRefundStatus.FAILED.getCode(), "渠道退款失败");
                notifyUser(
                        refundOrder.getUserId(),
                        fallbackActor(refundOrder),
                        "REFUND_FAILED",
                        String.format("退款处理失败，请稍后重试或联系平台：订单%s", refundOrder.getBizOrderNo())
                );
                return buildResponse(requireRefundOrder(refundOrder.getRefundNo()));
            }
            PaymentRefundResponse response = paymentRefundDomainService.markRefundSuccess(refundOrder, voucherOrder);
            insertReviewLog(refundOrder.getRefundNo(), REVIEWER_SYSTEM, null, "SYSTEM_SUCCESS",
                    PaymentRefundStatus.PROCESSING.getCode(), PaymentRefundStatus.SUCCESS.getCode(), "退款执行成功");
            notifyUser(
                    refundOrder.getUserId(),
                    fallbackActor(refundOrder),
                    "REFUND_SUCCESS",
                    String.format("退款已成功到账：订单%s，金额¥%s", refundOrder.getBizOrderNo(), refundOrder.getRefundAmount())
            );
            return response;
        } catch (Exception e) {
            logger.error("执行审核通过退款失败: refundNo={}", refundOrder.getRefundNo(), e);
            paymentRefundDomainService.markRefundFailed(refundOrder);
            insertReviewLog(refundOrder.getRefundNo(), REVIEWER_SYSTEM, null, "SYSTEM_FAILED",
                    PaymentRefundStatus.PROCESSING.getCode(), PaymentRefundStatus.FAILED.getCode(), e.getMessage());
            notifyUser(
                    refundOrder.getUserId(),
                    fallbackActor(refundOrder),
                    "REFUND_FAILED",
                    String.format("退款处理失败，请稍后重试或联系平台：订单%s", refundOrder.getBizOrderNo())
            );
            return buildResponse(requireRefundOrder(refundOrder.getRefundNo()));
        }
    }

    private void notifyAdmins(Long actorUserId, String type, String message) {
        Long safeActor = actorUserId == null ? 1L : actorUserId;
        List<Long> adminUserIds = userMapper.findUserIdsByRole(2);
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            return;
        }
        for (Long adminUserId : adminUserIds) {
            notifyUser(adminUserId, safeActor, type, message + "（管理员待办）");
        }
    }

    private void notifyUser(Long targetUserId, Long actorUserId, String type, String message) {
        if (targetUserId == null || actorUserId == null) {
            return;
        }
        messageNotificationService.createSystemNotification(
                type + ":" + targetUserId + ":" + UUID.randomUUID(),
                type,
                targetUserId,
                actorUserId,
                message
        );
    }

    private Long fallbackActor(PaymentRefundOrder refundOrder) {
        if (refundOrder.getAdminReviewerId() != null) {
            return refundOrder.getAdminReviewerId();
        }
        if (refundOrder.getMerchantReviewerId() != null) {
            return refundOrder.getMerchantReviewerId();
        }
        return refundOrder.getUserId();
    }

    private BigDecimal resolveApprovedAmount(PaymentRefundOrder refundOrder, PaymentOrder paymentOrder, BigDecimal approvedAmount) {
        BigDecimal baseRequestedAmount = refundOrder.getRequestedAmount() == null
                ? refundOrder.getRefundAmount()
                : refundOrder.getRequestedAmount();
        BigDecimal targetAmount = normalizeAmount(approvedAmount);
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            targetAmount = baseRequestedAmount;
        }
        if (targetAmount.compareTo(baseRequestedAmount) > 0) {
            throw new IllegalArgumentException("审核通过金额不能超过申请金额");
        }
        validateRefundableAmount(paymentOrder, refundOrder, targetAmount);
        return targetAmount;
    }

    private void validateRefundableAmount(PaymentOrder paymentOrder, PaymentRefundOrder currentRefundOrder, BigDecimal refundAmount) {
        if (paymentOrder == null) {
            throw new IllegalArgumentException("支付单不存在");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额必须大于 0");
        }
        BigDecimal refundedAmount = paymentRefundOrderMapper.sumSuccessfulRefundAmountByPaymentNo(paymentOrder.getPaymentNo());
        BigDecimal alreadyRefunded = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
        BigDecimal targetAmount = alreadyRefunded.add(refundAmount);
        if (currentRefundOrder != null && PaymentRefundStatus.SUCCESS.getCode().equalsIgnoreCase(currentRefundOrder.getStatus())) {
            targetAmount = targetAmount.subtract(currentRefundOrder.getRefundAmount());
        }
        if (targetAmount.compareTo(paymentOrder.getAmount()) > 0) {
            throw new IllegalArgumentException("退款金额超过可退金额");
        }
    }

    private void ensureOrderRefundable(VoucherOrder voucherOrder) {
        if (voucherOrder == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (voucherOrder.getStatus() == null || voucherOrder.getStatus() != 1 || voucherOrder.getUseTime() != null) {
            throw new IllegalArgumentException("仅未核销订单支持退款");
        }
    }

    private VoucherOrder requireVoucherOrder(String orderNo) {
        VoucherOrder voucherOrder = voucherOrderService.findByOrderNo(orderNo);
        if (voucherOrder == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return voucherOrder;
    }

    private Voucher requireVoucher(Long voucherId) {
        Voucher voucher = voucherMapper.findById(voucherId);
        if (voucher == null) {
            throw new IllegalArgumentException("券信息不存在");
        }
        if (voucher.getMerchantId() == null) {
            throw new IllegalArgumentException("券未绑定商家，暂不支持退款审核");
        }
        return voucher;
    }

    private PaymentOrder requirePaymentOrder(String paymentNo) {
        PaymentOrder paymentOrder = paymentOrderMapper.findByPaymentNo(paymentNo);
        if (paymentOrder == null) {
            throw new IllegalArgumentException("支付单不存在");
        }
        return paymentOrder;
    }

    private PaymentRefundOrder requireRefundOrder(String refundNo) {
        PaymentRefundOrder refundOrder = paymentRefundOrderMapper.findByRefundNo(refundNo);
        if (refundOrder == null) {
            throw new IllegalArgumentException("退款单不存在");
        }
        return refundOrder;
    }

    private Merchant requireVerifiedMerchant(Long merchantUserId) {
        Merchant merchant = merchantMapper.findByUserId(merchantUserId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new IllegalArgumentException("商家认证未通过，暂不可处理退款");
        }
        return merchant;
    }

    private void insertReviewLog(
            String refundNo,
            String operatorRole,
            Long operatorId,
            String action,
            String fromStatus,
            String toStatus,
            String comment
    ) {
        PaymentRefundReviewLog reviewLog = new PaymentRefundReviewLog();
        reviewLog.setRefundNo(refundNo);
        reviewLog.setOperatorRole(operatorRole);
        reviewLog.setOperatorId(operatorId);
        reviewLog.setAction(action);
        reviewLog.setFromStatus(fromStatus);
        reviewLog.setToStatus(toStatus);
        reviewLog.setComment(normalizeReason(comment));
        paymentRefundReviewLogMapper.insert(reviewLog);
    }

    private PaymentRefundResponse buildResponse(PaymentRefundOrder refundOrder) {
        PaymentRefundResponse response = new PaymentRefundResponse();
        response.setRefundNo(refundOrder.getRefundNo());
        response.setPaymentNo(refundOrder.getPaymentNo());
        response.setRefundAmount(refundOrder.getRefundAmount());
        response.setRequestedAmount(refundOrder.getRequestedAmount() == null ? refundOrder.getRefundAmount() : refundOrder.getRequestedAmount());
        response.setApprovedAmount(refundOrder.getApprovedAmount());
        response.setStatus(refundOrder.getStatus());
        response.setCurrentReviewerRole(refundOrder.getCurrentReviewerRole());
        return response;
    }

    private Map<String, Object> attachReviewLogs(Map<String, Object> detail, String refundNo) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (detail != null) {
            result.putAll(detail);
        }
        result.put("logs", paymentRefundReviewLogMapper.findByRefundNo(refundNo));
        return result;
    }

    private boolean isFinalStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return PaymentRefundStatus.SUCCESS.getCode().equalsIgnoreCase(status)
                || PaymentRefundStatus.FAILED.getCode().equalsIgnoreCase(status)
                || PaymentRefundStatus.REJECTED.getCode().equalsIgnoreCase(status)
                || PaymentRefundStatus.CLOSED.getCode().equalsIgnoreCase(status);
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros();
    }

    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(REFUND_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
