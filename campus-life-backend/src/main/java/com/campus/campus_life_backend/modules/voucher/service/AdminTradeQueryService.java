package com.campus.campus_life_backend.modules.voucher.service;

import com.campus.campus_life_backend.modules.voucher.entity.Voucher;
import com.campus.campus_life_backend.modules.voucher.mapper.VoucherMapper;
import com.campus.campus_life_backend.modules.voucher.vo.OrderTrendVO;
import com.campus.campus_life_backend.modules.order.mapper.VoucherOrderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminTradeQueryService {

    private final VoucherMapper voucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;

    public AdminTradeQueryService(VoucherMapper voucherMapper, VoucherOrderMapper voucherOrderMapper) {
        this.voucherMapper = voucherMapper;
        this.voucherOrderMapper = voucherOrderMapper;
    }

    public Map<String, Object> getVoucherList(Integer page, Integer size, Integer status, Long merchantId, String keyword) {
        String keywordLower = toLower(keyword);
        Long keywordId = tryParseLong(keyword);
        List<Voucher> filtered = findAllVouchersForAdmin().stream()
                .filter(v -> status == null || Objects.equals(v.getStatus(), status))
                .filter(v -> merchantId == null || Objects.equals(v.getMerchantId(), merchantId))
                .filter(v -> isBlank(keyword)
                        || Objects.equals(v.getId(), keywordId)
                        || containsIgnoreCase(v.getTitle(), keywordLower)
                        || containsIgnoreCase(v.getSubTitle(), keywordLower))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(filtered, page, size));
        result.put("page", normalizePage(page));
        result.put("size", normalizeSize(size));
        result.put("total", filtered.size());
        return result;
    }

    public Map<String, Object> getVoucherOrderList(
            Integer page,
            Integer size,
            String keyword,
            Long userId,
            Long voucherId,
            Integer status,
            Integer paymentStatus,
            String orderSource,
            String timeRange,
            String startDate,
            String endDate
    ) {
        int safePage = normalizePage(page);
        int safeSize = Math.min(Math.max(size == null ? 20 : size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        LocalDateTime[] range = resolveTimeRange(timeRange, startDate, endDate);

        String safeKeyword = isBlank(keyword) ? null : keyword.trim();
        String safeOrderSource = isBlank(orderSource) ? null : orderSource.trim();
        List<Map<String, Object>> list = voucherOrderMapper.findAdminOrderViews(
                safeKeyword, userId, voucherId, status, paymentStatus, safeOrderSource, range[0], range[1], offset, safeSize
        );
        Integer total = voucherOrderMapper.countAdminOrderViews(
                safeKeyword, userId, voucherId, status, paymentStatus, safeOrderSource, range[0], range[1]
        );

        if (list != null) {
            list.forEach(this::appendOrderStatusFields);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list == null ? List.of() : list);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public Map<String, Object> getVoucherOrderStats(
            String keyword,
            Long userId,
            Long voucherId,
            Integer status,
            Integer paymentStatus,
            String orderSource,
            String timeRange,
            String startDate,
            String endDate
    ) {
        LocalDateTime[] range = resolveTimeRange(timeRange, startDate, endDate);
        String safeKeyword = isBlank(keyword) ? null : keyword.trim();
        String safeOrderSource = isBlank(orderSource) ? null : orderSource.trim();
        Map<String, Object> summary = voucherOrderMapper.sumAdminOrderStats(
                safeKeyword, userId, voucherId, status, paymentStatus, safeOrderSource, range[0], range[1]
        );
        List<Map<String, Object>> trend = voucherOrderMapper.findAdminOrderTrend(range[0], range[1]);
        List<Map<String, Object>> voucherUsage = voucherOrderMapper.findAdminVoucherUsage(range[0], range[1], 10);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary == null ? Map.of() : summary);
        result.put("trend", trend == null ? List.of() : trend);
        result.put("voucherUsage", voucherUsage == null ? List.of() : voucherUsage);
        result.put("timeRange", Map.of("start", range[0], "end", range[1]));
        return result;
    }

    public List<Voucher> findAllVouchersForAdmin() {
        List<Voucher> vouchers = voucherMapper.findAll();
        return vouchers == null ? List.of() : vouchers;
    }

    public long countVouchers() {
        return voucherMapper.countAllForAdmin();
    }

    public long countVouchersByStatus(Integer status) {
        return voucherMapper.countByStatusForAdmin(status);
    }

    public long countVouchersSince(LocalDateTime since) {
        return voucherMapper.countVouchersSince(since);
    }

    public long countOrdersSince(LocalDateTime since) {
        return voucherOrderMapper.countOrdersSince(since);
    }

    public List<Map<String, Object>> countVoucherTypeData() {
        List<Map<String, Object>> rows = voucherMapper.countByMerchantTypeForAdmin();
        return rows == null ? List.of() : rows;
    }

    public List<Map<String, Object>> countOrdersTrend(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> rows = voucherOrderMapper.countOrdersTrend(startTime, endTime);
        return rows == null ? List.of() : rows;
    }

    public List<OrderTrendVO> countOrdersTrend(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(Math.max(days, 1) - 1L).toLocalDate().atStartOfDay();
        return countOrdersTrend(start, end).stream()
                .map(row -> new OrderTrendVO(String.valueOf(row.get("day")), toLong(row.get("value"))))
                .toList();
    }

    public List<Map<String, Object>> countVoucherContributionByMonthWeek(LocalDateTime startTime) {
        List<Map<String, Object>> rows = voucherMapper.countContributionByMonthWeek(startTime);
        return rows == null ? List.of() : rows;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private <T> List<T> paginate(List<T> source, Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int from = (p - 1) * s;
        if (from >= source.size()) {
            return List.of();
        }
        int to = Math.min(from + s, source.size());
        return new ArrayList<>(source.subList(from, to));
    }

    private boolean containsIgnoreCase(String value, String expectedLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(expectedLower);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String toLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long tryParseLong(String value) {
        if (isBlank(value)) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime[] resolveTimeRange(String timeRange, String startDate, String endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate startLocal = parseDate(startDate);
        LocalDate endLocal = parseDate(endDate);
        if (startLocal != null || endLocal != null) {
            LocalDateTime start = startLocal == null ? now.minusYears(3) : startLocal.atStartOfDay();
            LocalDateTime end = endLocal == null ? now : endLocal.atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }
        String range = isBlank(timeRange) ? "today" : timeRange.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        if ("week".equals(range)) {
            LocalDate begin = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            return new LocalDateTime[]{begin.atStartOfDay(), now};
        }
        if ("month".equals(range)) {
            LocalDate begin = today.withDayOfMonth(1);
            return new LocalDateTime[]{begin.atStartOfDay(), now};
        }
        if ("quarter".equals(range)) {
            int month = today.getMonthValue();
            int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
            LocalDate begin = LocalDate.of(today.getYear(), quarterStartMonth, 1);
            return new LocalDateTime[]{begin.atStartOfDay(), now};
        }
        if ("year".equals(range)) {
            LocalDate begin = LocalDate.of(today.getYear(), 1, 1);
            return new LocalDateTime[]{begin.atStartOfDay(), now};
        }
        return new LocalDateTime[]{today.atStartOfDay(), now};
    }

    private void appendOrderStatusFields(Map<String, Object> order) {
        int status = parseInteger(order.get("status"), 0);
        order.put("statusText", toOrderStatusText(status));
        order.put("statusClass", toOrderStatusClass(status));
    }

    private String toOrderStatusText(int status) {
        if (status == 0) return "待支付";
        if (status == 1) return "待使用";
        if (status == 2) return "已完成";
        if (status == 3) return "已过期";
        if (status == 4) return "已退款";
        if (status == 5) return "已取消";
        return "未知";
    }

    private String toOrderStatusClass(int status) {
        if (status == 0) return "pending";
        if (status == 1) return "paid";
        if (status == 2) return "used";
        if (status == 3) return "expired";
        if (status == 4) return "refunded";
        if (status == 5) return "cancelled";
        return "unknown";
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number num) return num.intValue();
            String str = String.valueOf(value).trim();
            if (str.isEmpty()) return defaultValue;
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }
}
