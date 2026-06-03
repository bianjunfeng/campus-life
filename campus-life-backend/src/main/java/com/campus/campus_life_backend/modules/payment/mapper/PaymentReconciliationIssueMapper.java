package com.campus.campus_life_backend.modules.payment.mapper;

import com.campus.campus_life_backend.modules.payment.entity.PaymentReconciliationIssue;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PaymentReconciliationIssueMapper {

    PaymentReconciliationIssue findByIssueKey(@Param("issueKey") String issueKey);

    List<PaymentReconciliationIssue> findOpenIssuesByPaymentOrBiz(
            @Param("paymentNo") String paymentNo,
            @Param("bizOrderNo") String bizOrderNo
    );

    int insert(PaymentReconciliationIssue issue);

    int updateOpenIssue(PaymentReconciliationIssue issue);

    int markResolved(
            @Param("id") Long id,
            @Param("resolvedTime") LocalDateTime resolvedTime,
            @Param("resolvedBy") Long resolvedBy,
            @Param("resolveNote") String resolveNote
    );

    List<Map<String, Object>> findIssueViews(
            @Param("status") String status,
            @Param("issueType") String issueType
    );

    Map<String, Object> findIssueSummary();

    List<Map<String, Object>> findPaymentOrderCandidates(
            @Param("sinceTime") LocalDateTime sinceTime,
            @Param("limit") Integer limit
    );

    List<Map<String, Object>> findPaidVoucherOrdersMissingPayment(
            @Param("sinceTime") LocalDateTime sinceTime,
            @Param("limit") Integer limit
    );
}
