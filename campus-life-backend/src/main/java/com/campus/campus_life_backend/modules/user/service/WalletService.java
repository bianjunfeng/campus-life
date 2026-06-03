package com.campus.campus_life_backend.modules.user.service;

import com.campus.campus_life_backend.modules.user.mapper.UserWalletMapper;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private static final BigDecimal DEFAULT_WALLET_BALANCE = new BigDecimal("1000.00");

    private final UserWalletMapper userWalletMapper;
    private final VoucherOrderMapper voucherOrderMapper;

    public WalletService(UserWalletMapper userWalletMapper, VoucherOrderMapper voucherOrderMapper) {
        this.userWalletMapper = userWalletMapper;
        this.voucherOrderMapper = voucherOrderMapper;
    }

    public Map<String, Object> getWalletOverview(Long userId) {
        userWalletMapper.initWalletIfAbsent(userId, DEFAULT_WALLET_BALANCE);

        Map<String, Object> wallet = userWalletMapper.findByUserId(userId);
        BigDecimal totalSpent = toBigDecimal(wallet != null ? wallet.get("totalSpent") : null);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        int pendingCount = safeInt(voucherOrderMapper.countPendingPaymentByUserId(userId));
        int availableCouponCount = safeInt(voucherOrderMapper.countAvailableCouponByUserId(userId));

        BigDecimal availableBalance = toBigDecimal(wallet != null ? wallet.get("balance") : null);
        if (availableBalance == null) {
            availableBalance = DEFAULT_WALLET_BALANCE.subtract(totalSpent);
            if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
                availableBalance = BigDecimal.ZERO;
            }
        }

        List<Map<String, Object>> recentTransactions = voucherOrderMapper.findRecentTransactionsByUserId(userId, 10);
        for (Map<String, Object> tx : recentTransactions) {
            Object orderStatus = tx.get("orderStatus");
            int statusCode = orderStatus instanceof Number ? ((Number) orderStatus).intValue() : 0;
            tx.put("statusText", toStatusText(statusCode));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("availableBalance", availableBalance.setScale(2, RoundingMode.HALF_UP));
        result.put("totalSpent", totalSpent.setScale(2, RoundingMode.HALF_UP));
        result.put("pendingPaymentCount", pendingCount);
        result.put("availableCouponCount", availableCouponCount);
        result.put("recentTransactions", recentTransactions);
        return result;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toStatusText(int statusCode) {
        if (statusCode == 0) return "待支付";
        if (statusCode == 1) return "待使用";
        if (statusCode == 2) return "已使用";
        if (statusCode == 3) return "已过期";
        if (statusCode == 4) return "已退款";
        if (statusCode == 5) return "已取消";
        return "未知状态";
    }
}
