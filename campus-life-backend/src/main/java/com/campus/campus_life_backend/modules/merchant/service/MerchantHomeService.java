package com.campus.campus_life_backend.modules.merchant.service;

import com.campus.campus_life_backend.modules.merchant.entity.Merchant;
import com.campus.campus_life_backend.modules.merchant.entity.MerchantType;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantMapper;
import com.campus.campus_life_backend.modules.merchant.mapper.MerchantTypeMapper;
import com.campus.campus_life_backend.modules.payment.enums.PaymentRefundStatus;
import com.campus.campus_life_backend.modules.payment.mapper.PaymentRefundOrderMapper;
import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MerchantHomeService {

    private final MerchantMapper merchantMapper;
    private final MerchantTypeMapper merchantTypeMapper;
    private final VoucherMapper voucherMapper;
    private final PaymentRefundOrderMapper paymentRefundOrderMapper;

    public MerchantHomeService(
            MerchantMapper merchantMapper,
            MerchantTypeMapper merchantTypeMapper,
            VoucherMapper voucherMapper,
            PaymentRefundOrderMapper paymentRefundOrderMapper
    ) {
        this.merchantMapper = merchantMapper;
        this.merchantTypeMapper = merchantTypeMapper;
        this.voucherMapper = voucherMapper;
        this.paymentRefundOrderMapper = paymentRefundOrderMapper;
    }

    public Map<String, Object> getMerchantHome(Long merchantUserId) {
        Merchant merchant = requireVerifiedMerchant(merchantUserId);
        MerchantType merchantType = merchant.getTypeId() == null ? null : merchantTypeMapper.findById(merchant.getTypeId());
        List<Voucher> vouchers = defaultList(voucherMapper.findAllByMerchantId(merchant.getId()));
        List<Map<String, Object>> refunds = defaultList(paymentRefundOrderMapper.findMerchantRefundViews(merchant.getId(), null));

        long totalVouchers = vouchers.size();
        long onlineVouchers = vouchers.stream()
                .filter(voucher -> voucher.getStatus() != null && voucher.getStatus() == 1)
                .count();
        long offlineVouchers = vouchers.stream()
                .filter(voucher -> voucher.getStatus() == null || voucher.getStatus() == 0)
                .count();
        long totalSoldCount = vouchers.stream()
                .map(Voucher::getSoldCount)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();
        long pendingRefunds = countRefunds(refunds, PaymentRefundStatus.WAIT_MERCHANT_REVIEW.getCode());
        long escalatedRefunds = countRefunds(refunds, PaymentRefundStatus.WAIT_ADMIN_REVIEW.getCode());
        long processingRefunds = countRefunds(refunds, PaymentRefundStatus.PROCESSING.getCode());
        long successRefunds = countRefunds(refunds, PaymentRefundStatus.SUCCESS.getCode());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("merchant", buildMerchantInfo(merchant, merchantType));
        result.put("managementHome", buildManagementHome(
                totalVouchers,
                onlineVouchers,
                offlineVouchers,
                totalSoldCount,
                pendingRefunds,
                escalatedRefunds,
                processingRefunds,
                successRefunds
        ));
        result.put("recentVouchers", buildRecentVouchers(vouchers));
        result.put("recentRefunds", buildRecentRefunds(refunds));
        result.put("serverTime", LocalDateTime.now());
        return result;
    }

    private Merchant requireVerifiedMerchant(Long merchantUserId) {
        Merchant merchant = merchantMapper.findByUserId(merchantUserId);
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new IllegalArgumentException("商家认证未通过，暂不可访问商家首页");
        }
        return merchant;
    }

    private Map<String, Object> buildMerchantInfo(Merchant merchant, MerchantType merchantType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", merchant.getId());
        result.put("name", merchant.getName());
        result.put("status", merchant.getStatus());
        result.put("statusLabel", resolveMerchantStatusLabel(merchant.getStatus()));
        result.put("typeId", merchant.getTypeId());
        result.put("typeName", merchantType == null ? null : merchantType.getName());
        result.put("contactName", merchant.getContactName());
        result.put("contactPhone", merchant.getContactPhone());
        result.put("address", merchant.getAddress());
        result.put("businessHours", merchant.getBusinessHours());
        result.put("province", merchant.getProvince());
        result.put("city", merchant.getCity());
        result.put("district", merchant.getDistrict());
        result.put("locationStatus", merchant.getLocationStatus());
        result.put("locationStatusLabel", resolveLocationStatusLabel(merchant.getLocationStatus()));
        result.put("locationUpdatedTime", merchant.getLocationUpdatedTime());
        result.put("createTime", merchant.getCreateTime());
        return result;
    }

    private Map<String, Object> buildManagementHome(
            long totalVouchers,
            long onlineVouchers,
            long offlineVouchers,
            long totalSoldCount,
            long pendingRefunds,
            long escalatedRefunds,
            long processingRefunds,
            long successRefunds
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", "/merchant/home");
        result.put("title", "管理端首页");
        result.put("description", "处理券商品、退款审核和店铺运营数据。");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVouchers", totalVouchers);
        stats.put("onlineVouchers", onlineVouchers);
        stats.put("offlineVouchers", offlineVouchers);
        stats.put("totalSoldCount", totalSoldCount);
        stats.put("pendingRefunds", pendingRefunds);
        stats.put("escalatedRefunds", escalatedRefunds);
        stats.put("processingRefunds", processingRefunds);
        stats.put("successRefunds", successRefunds);
        result.put("stats", stats);

        result.put("quickActions", List.of(
                action("vouchers", "优惠券管理", "/merchant/vouchers", "创建、上下架和编辑商品券。"),
                action("refunds", "退款审核", "/merchant/refunds", "优先处理待审核和升级平台的退款单。"),
                action("ai", "AI 工作台", "/merchant/ai", "进入商家对话、知识库和运营 Agent。")
        ));
        return result;
    }

    private List<Map<String, Object>> buildRecentVouchers(List<Voucher> vouchers) {
        return vouchers.stream()
                .limit(3)
                .map(voucher -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", voucher.getId());
                    item.put("title", voucher.getTitle());
                    item.put("status", voucher.getStatus());
                    item.put("statusLabel", voucher.getStatus() != null && voucher.getStatus() == 1 ? "上架中" : "已下架");
                    item.put("stock", voucher.getStock());
                    item.put("soldCount", voucher.getSoldCount());
                    item.put("updateTime", voucher.getUpdateTime());
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> buildRecentRefunds(List<Map<String, Object>> refunds) {
        return refunds.stream()
                .limit(3)
                .map(refund -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("refundNo", refund.get("refundNo"));
                    item.put("voucherTitle", refund.get("voucherTitle"));
                    item.put("status", refund.get("status"));
                    item.put("statusLabel", resolveRefundStatusLabel(refund.get("status")));
                    item.put("requestedAmount", refund.get("requestedAmount"));
                    item.put("createTime", refund.get("createTime"));
                    return item;
                })
                .toList();
    }

    private long countRefunds(List<Map<String, Object>> refunds, String expectedStatus) {
        return refunds.stream()
                .filter(refund -> expectedStatus.equalsIgnoreCase(String.valueOf(refund.get("status"))))
                .count();
    }

    private Map<String, Object> action(String key, String label, String path, String description) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("key", key);
        action.put("label", label);
        action.put("path", path);
        action.put("description", description);
        return action;
    }

    private String resolveMerchantStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "正常";
            case 2 -> "冻结";
            case 3 -> "关闭";
            default -> "未知";
        };
    }

    private String resolveLocationStatusLabel(Integer locationStatus) {
        if (locationStatus == null || locationStatus == 0) {
            return "未设置";
        }
        return locationStatus == 1 ? "已定位" : "未知";
    }

    private String resolveRefundStatusLabel(Object status) {
        String normalized = String.valueOf(status);
        return switch (normalized) {
            case "WAIT_MERCHANT_REVIEW" -> "待商家审核";
            case "WAIT_ADMIN_REVIEW" -> "待平台审核";
            case "PROCESSING" -> "退款处理中";
            case "SUCCESS" -> "退款成功";
            case "FAILED" -> "退款失败";
            case "REJECTED" -> "已驳回";
            case "CLOSED" -> "已关闭";
            default -> normalized;
        };
    }

    private <T> List<T> defaultList(List<T> source) {
        return source == null ? List.of() : source;
    }
}
