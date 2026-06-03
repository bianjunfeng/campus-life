package com.campus.campus_life_backend.modules.payment.service;

import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.modules.message.service.MessageNotificationService;
import com.campus.campus_life_backend.modules.payment.entity.PaymentReconciliationIssue;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentReconciliationIssueMapper;
import com.campus.campus_life_backend.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentReconciliationService {

    private static final String ISSUE_STATUS_OPEN = "OPEN";
    private static final int ADMIN_ROLE = 2;

    private final PaymentReconciliationIssueMapper issueMapper;
    private final MessageNotificationService messageNotificationService;
    private final UserMapper userMapper;

    public PaymentReconciliationService(
            PaymentReconciliationIssueMapper issueMapper,
            MessageNotificationService messageNotificationService,
            UserMapper userMapper
    ) {
        this.issueMapper = issueMapper;
        this.messageNotificationService = messageNotificationService;
        this.userMapper = userMapper;
    }

    @RequirePermission(anyOf = {"admin:payment:reconcile"})
    public Map<String, Object> listIssues(String status, String issueType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", issueMapper.findIssueSummary());
        result.put("list", issueMapper.findIssueViews(normalize(status), normalize(issueType)));
        return result;
    }

    @Transactional
    @RequirePermission(anyOf = {"admin:payment:reconcile"})
    public Map<String, Object> scanRecentIssues(int days, int limit) {
        return doScanRecentIssues(days, limit);
    }

    @Transactional
    public Map<String, Object> scanRecentIssuesForTask(int days, int limit) {
        return doScanRecentIssues(days, limit);
    }

    private Map<String, Object> doScanRecentIssues(int days, int limit) {
        int safeDays = days <= 0 ? 7 : days;
        int safeLimit = limit <= 0 ? 500 : limit;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sinceTime = now.minusDays(safeDays);

        int opened = 0;
        int resolved = 0;
        int scanned = 0;

        List<Map<String, Object>> paymentCandidates = issueMapper.findPaymentOrderCandidates(sinceTime, safeLimit);
        for (Map<String, Object> candidate : paymentCandidates) {
            scanned++;
            ResultDelta delta = syncIssues(
                    stringValue(candidate.get("paymentNo")),
                    stringValue(candidate.get("bizOrderNo")),
                    buildIssuesForPaymentCandidate(candidate, now),
                    now
            );
            opened += delta.opened;
            resolved += delta.resolved;
        }

        List<Map<String, Object>> missingPaymentCandidates = issueMapper.findPaidVoucherOrdersMissingPayment(sinceTime, safeLimit);
        for (Map<String, Object> candidate : missingPaymentCandidates) {
            scanned++;
            ResultDelta delta = syncIssues(
                    null,
                    stringValue(candidate.get("bizOrderNo")),
                    Collections.singletonList(buildMissingPaymentIssue(candidate, now)),
                    now
            );
            opened += delta.opened;
            resolved += delta.resolved;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", safeDays);
        result.put("scannedCount", scanned);
        result.put("openedCount", opened);
        result.put("resolvedCount", resolved);
        result.put("summary", issueMapper.findIssueSummary());
        return result;
    }

    @Transactional
    @RequirePermission(anyOf = {"admin:payment:reconcile"})
    public void resolveIssue(Long issueId, Long adminId, String note) {
        int updated = issueMapper.markResolved(issueId, LocalDateTime.now(), adminId, normalizeNote(note, "管理员手动标记已处理"));
        if (updated <= 0) {
            throw new IllegalArgumentException("差错单不存在");
        }
    }

    private ResultDelta syncIssues(String paymentNo, String bizOrderNo, List<PaymentReconciliationIssue> currentIssues, LocalDateTime now) {
        ResultDelta delta = new ResultDelta();
        Map<String, PaymentReconciliationIssue> currentIssueMap = new LinkedHashMap<>();
        for (PaymentReconciliationIssue issue : currentIssues) {
            currentIssueMap.put(issue.getIssueKey(), issue);
            PaymentReconciliationIssue existing = issueMapper.findByIssueKey(issue.getIssueKey());
            if (existing == null) {
                issueMapper.insert(issue);
                notifyAdminsIfCritical(issue, true);
                delta.opened++;
            } else {
                issue.setId(existing.getId());
                issueMapper.updateOpenIssue(issue);
                if (!ISSUE_STATUS_OPEN.equalsIgnoreCase(existing.getIssueStatus())) {
                    notifyAdminsIfCritical(issue, false);
                    delta.opened++;
                }
            }
        }

        List<PaymentReconciliationIssue> existingOpenIssues = issueMapper.findOpenIssuesByPaymentOrBiz(paymentNo, bizOrderNo);
        for (PaymentReconciliationIssue existing : existingOpenIssues) {
            if (!currentIssueMap.containsKey(existing.getIssueKey())) {
                issueMapper.markResolved(existing.getId(), now, null, "系统扫描确认已恢复一致");
                delta.resolved++;
            }
        }
        return delta;
    }

    private List<PaymentReconciliationIssue> buildIssuesForPaymentCandidate(Map<String, Object> candidate, LocalDateTime now) {
        List<PaymentReconciliationIssue> issues = new ArrayList<>();

        String paymentNo = stringValue(candidate.get("paymentNo"));
        String bizOrderNo = stringValue(candidate.get("bizOrderNo"));
        String paymentOrderStatus = stringValue(candidate.get("paymentOrderStatus"));
        Integer voucherOrderStatus = intValue(candidate.get("voucherOrderStatus"));
        Integer voucherPaymentStatus = intValue(candidate.get("voucherPaymentStatus"));
        BigDecimal paymentAmount = decimalValue(candidate.get("paymentAmount"));
        BigDecimal recordedRefundedAmount = decimalValue(candidate.get("recordedRefundedAmount"));
        BigDecimal actualRefundedAmount = decimalValue(candidate.get("actualRefundedAmount"));

        if ("SUCCESS".equalsIgnoreCase(paymentOrderStatus)
                && (voucherPaymentStatus == null || (voucherPaymentStatus != 1 && voucherPaymentStatus != 3))) {
            issues.add(buildIssue("PAYMENT_SUCCESS_ORDER_STATE_MISMATCH:" + paymentNo, paymentNo, bizOrderNo, candidate,
                    "PAYMENT_SUCCESS_ORDER_STATE_MISMATCH", "CRITICAL", "支付单已成功，但业务订单未标记为已支付/已退款", now));
        }

        if (voucherPaymentStatus != null
                && voucherPaymentStatus == 1
                && !"SUCCESS".equalsIgnoreCase(paymentOrderStatus)
                && !"PARTIAL_REFUNDED".equalsIgnoreCase(paymentOrderStatus)
                && !"FULL_REFUNDED".equalsIgnoreCase(paymentOrderStatus)) {
            issues.add(buildIssue("ORDER_PAID_PAYMENT_STATUS_MISMATCH:" + paymentNo, paymentNo, bizOrderNo, candidate,
                    "ORDER_PAID_PAYMENT_STATUS_MISMATCH", "CRITICAL", "业务订单已支付，但支付单状态未成功", now));
        }

        if (recordedRefundedAmount.compareTo(actualRefundedAmount) != 0) {
            issues.add(buildIssue("REFUND_AMOUNT_MISMATCH:" + paymentNo, paymentNo, bizOrderNo, candidate,
                    "REFUND_AMOUNT_MISMATCH", "WARN", "支付单记录退款金额与成功退款累计金额不一致", now));
        }

        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0
                && actualRefundedAmount.compareTo(paymentAmount) >= 0
                && (voucherPaymentStatus == null || voucherPaymentStatus != 3 || voucherOrderStatus == null || voucherOrderStatus != 4)) {
            issues.add(buildIssue("FULL_REFUND_ORDER_STATE_MISMATCH:" + paymentNo, paymentNo, bizOrderNo, candidate,
                    "FULL_REFUND_ORDER_STATE_MISMATCH", "CRITICAL", "退款已达到全额，但业务订单未标记为已退款", now));
        }

        return issues;
    }

    private PaymentReconciliationIssue buildMissingPaymentIssue(Map<String, Object> candidate, LocalDateTime now) {
        PaymentReconciliationIssue issue = buildIssue(
                "MISSING_PAYMENT_ORDER:" + stringValue(candidate.get("bizOrderNo")),
                null,
                stringValue(candidate.get("bizOrderNo")),
                candidate,
                "MISSING_PAYMENT_ORDER",
                "CRITICAL",
                "业务订单已支付/已退款，但缺少 payment_order 记录",
                now
        );
        issue.setPaymentOrderStatus(null);
        issue.setRecordedRefundedAmount(BigDecimal.ZERO);
        issue.setActualRefundedAmount(BigDecimal.ZERO);
        return issue;
    }

    private PaymentReconciliationIssue buildIssue(
            String issueKey,
            String paymentNo,
            String bizOrderNo,
            Map<String, Object> candidate,
            String issueType,
            String issueLevel,
            String issueMessage,
            LocalDateTime now
    ) {
        PaymentReconciliationIssue issue = new PaymentReconciliationIssue();
        issue.setIssueKey(issueKey);
        issue.setBizType(stringValue(candidate.getOrDefault("bizType", "VOUCHER")));
        issue.setPaymentNo(paymentNo);
        issue.setBizOrderNo(bizOrderNo);
        issue.setUserId(longValue(candidate.get("userId")));
        issue.setChannel(stringValue(candidate.get("channel")));
        issue.setIssueType(issueType);
        issue.setIssueLevel(issueLevel);
        issue.setIssueStatus(ISSUE_STATUS_OPEN);
        issue.setIssueMessage(issueMessage);
        issue.setPaymentOrderStatus(stringValue(candidate.get("paymentOrderStatus")));
        issue.setVoucherOrderStatus(intValue(candidate.get("voucherOrderStatus")));
        issue.setVoucherPaymentStatus(intValue(candidate.get("voucherPaymentStatus")));
        issue.setPaymentAmount(decimalValue(candidate.get("paymentAmount")));
        issue.setRecordedRefundedAmount(decimalValue(candidate.get("recordedRefundedAmount")));
        issue.setActualRefundedAmount(decimalValue(candidate.get("actualRefundedAmount")));
        issue.setLastCheckedTime(now);
        issue.setResolvedTime(null);
        issue.setResolvedBy(null);
        issue.setResolveNote(null);
        return issue;
    }

    private void notifyAdminsIfCritical(PaymentReconciliationIssue issue, boolean freshCreated) {
        if (issue == null || !"CRITICAL".equalsIgnoreCase(issue.getIssueLevel())) {
            return;
        }
        List<Long> adminUserIds = userMapper.findUserIdsByRole(ADMIN_ROLE);
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            return;
        }
        Long actorUserId = issue.getUserId() == null ? 1L : issue.getUserId();
        String prefix = freshCreated ? "新增严重支付差错" : "支付差错再次打开";
        String message = String.format(
                "%s：订单%s，类型%s，请尽快前往支付对账处理",
                prefix,
                issue.getBizOrderNo() == null ? "-" : issue.getBizOrderNo(),
                issue.getIssueMessage()
        );
        for (Long adminUserId : adminUserIds) {
            messageNotificationService.createSystemNotification(
                    "PAYMENT_RECON_ALERT:" + adminUserId + ":" + issue.getIssueKey() + ":" + (freshCreated ? "OPEN" : "REOPEN"),
                    "PAYMENT_RECON_ALERT",
                    adminUserId,
                    actorUserId,
                    message
            );
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeNote(String note, String fallback) {
        if (note == null || note.isBlank()) {
            return fallback;
        }
        return note.trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    private static class ResultDelta {
        private int opened;
        private int resolved;
    }
}
