package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.admin.vo.DashboardStatsVO;
import com.campus.campus_life_backend.modules.admin.vo.TodayStatsVO;
import com.campus.campus_life_backend.modules.forum.service.AdminCommunityQueryService;
import com.campus.campus_life_backend.modules.merchant.service.MerchantQueryService;
import com.campus.campus_life_backend.modules.payment.service.PaymentRefundWorkflowService;
import com.campus.campus_life_backend.modules.user.service.AdminUserQueryService;
import com.campus.campus_life_backend.modules.voucher.service.AdminTradeQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardQueryService {

    private final AdminUserQueryService userQueryService;
    private final AdminCommunityQueryService communityQueryService;
    private final MerchantQueryService merchantQueryService;
    private final AdminTradeQueryService tradeQueryService;
    private final AdminOperationLogMapper operationLogMapper;
    private final PaymentRefundWorkflowService refundWorkflowService;

    public AdminDashboardQueryService(
            AdminUserQueryService userQueryService,
            AdminCommunityQueryService communityQueryService,
            MerchantQueryService merchantQueryService,
            AdminTradeQueryService tradeQueryService,
            AdminOperationLogMapper operationLogMapper,
            PaymentRefundWorkflowService refundWorkflowService) {
        this.userQueryService = userQueryService;
        this.communityQueryService = communityQueryService;
        this.merchantQueryService = merchantQueryService;
        this.tradeQueryService = tradeQueryService;
        this.operationLogMapper = operationLogMapper;
        this.refundWorkflowService = refundWorkflowService;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = userQueryService.countUsersByRoleAndStatus(0, null);
        long approvedStudents = userQueryService.countVerifiedStudents();
        long rejectedStudents = userQueryService.countUsersByRoleAndStatus(0, 0);
        Map<String, Object> studentAuth = new HashMap<>();
        studentAuth.put("pending", Math.max(totalStudents - approvedStudents - rejectedStudents, 0));
        studentAuth.put("approved", approvedStudents);
        studentAuth.put("rejected", rejectedStudents);
        stats.put("studentAuth", studentAuth);

        Map<String, Object> merchantAuth = new HashMap<>();
        merchantAuth.put("pending", merchantQueryService.countMerchantsByStatus(0));
        merchantAuth.put("approved", merchantQueryService.countMerchantsByStatus(1));
        merchantAuth.put("rejected", merchantQueryService.countMerchantsByStatus(3));
        stats.put("merchantAuth", merchantAuth);

        Map<String, Object> posts = new HashMap<>();
        posts.put("total", communityQueryService.countPosts());
        posts.put("pending", communityQueryService.countPostsByStatus(1));
        posts.put("banned", communityQueryService.countPostsByStatus(3));
        stats.put("posts", posts);

        Map<String, Object> comments = new HashMap<>();
        comments.put("total", communityQueryService.countComments());
        comments.put("pending", communityQueryService.countCommentsByStatus(1));
        comments.put("banned", communityQueryService.countCommentsByStatus(2));
        stats.put("comments", comments);

        Map<String, Object> merchants = new HashMap<>();
        merchants.put("total", merchantQueryService.countMerchants());
        merchants.put("normal", merchantQueryService.countMerchantsByStatus(1));
        merchants.put("frozen", merchantQueryService.countMerchantsByStatus(2));
        stats.put("merchants", merchants);

        Map<String, Object> vouchers = new HashMap<>();
        vouchers.put("total", tradeQueryService.countVouchers());
        vouchers.put("online", tradeQueryService.countVouchersByStatus(1));
        vouchers.put("offline", tradeQueryService.countVouchersByStatus(0));
        stats.put("vouchers", vouchers);

        return stats;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayIP", getTodayDistinctIpCount());
        result.put("memberCount", userQueryService.countUsers());
        result.put("postCount", communityQueryService.countPosts());
        result.put("voucherCount", tradeQueryService.countVouchers());
        result.put("merchantCount", merchantQueryService.countMerchants());
        result.put("contributionData", buildContributionData());
        result.put("contentTypeData", toPieData(communityQueryService.countContentTypeData()));
        result.put("voucherTypeData", toPieData(tradeQueryService.countVoucherTypeData()));
        result.put("merchantTypeData", toPieData(merchantQueryService.countMerchantTypeData()));
        return result;
    }

    public Map<String, Object> getTodayStats() {
        TodayStatsVO stats = buildTodayStats();
        Map<String, Object> result = new HashMap<>();
        result.put("newUsersToday", stats.getNewUsersToday());
        result.put("newPostsToday", stats.getNewPostsToday());
        result.put("newOrdersToday", stats.getNewOrdersToday());
        result.put("newMerchantsToday", stats.getNewMerchantsToday());
        result.putAll(toDashboardStatsMap(stats));
        return result;
    }

    public DashboardStatsVO getDashboardStatsVO() {
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setTotalUsers(userQueryService.countUsers());
        stats.setTotalPosts(communityQueryService.countPosts());
        stats.setTotalMerchants(merchantQueryService.countMerchants());
        stats.setTotalVouchers(tradeQueryService.countVouchers());
        stats.setPendingStudentAuth(userQueryService.countPendingStudentAuth());
        stats.setPendingMerchantAuth(merchantQueryService.countPendingMerchantAuth());
        stats.setPendingReports(communityQueryService.countPendingReports());
        stats.setPendingRefunds(refundWorkflowService.countPendingAdminRefunds());
        return stats;
    }

    public TodayStatsVO buildTodayStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        DashboardStatsVO base = getDashboardStatsVO();
        TodayStatsVO stats = new TodayStatsVO();
        stats.setTotalUsers(base.getTotalUsers());
        stats.setTotalPosts(base.getTotalPosts());
        stats.setTotalMerchants(base.getTotalMerchants());
        stats.setTotalVouchers(base.getTotalVouchers());
        stats.setPendingStudentAuth(base.getPendingStudentAuth());
        stats.setPendingMerchantAuth(base.getPendingMerchantAuth());
        stats.setPendingReports(base.getPendingReports());
        stats.setPendingRefunds(base.getPendingRefunds());
        stats.setNewUsersToday(userQueryService.countUsersSince(todayStart));
        stats.setNewPostsToday(communityQueryService.countPostsSince(todayStart));
        stats.setNewOrdersToday(tradeQueryService.countOrdersSince(todayStart));
        stats.setNewMerchantsToday(merchantQueryService.countMerchantsSince(todayStart));
        return stats;
    }

    private long getTodayDistinctIpCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        Long count = operationLogMapper.countDistinctIpBetween(start.toString(), end.toString());
        return count == null ? 0L : count;
    }

    private Map<String, Object> buildContributionData() {
        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(11);
        List<String> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            months.add(ym.getMonthValue() + "月");
        }
        List<String> weeks = List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        int[][] bucket = new int[12][7];

        for (Map<String, Object> row : communityQueryService.countContributionByMonthWeek(start.atDay(1).atStartOfDay())) {
            String month = String.valueOf(row.get("month"));
            int monthIndex = parseMonthIndex(month, start);
            int weekIndex = toInt(row.get("weekIndex"));
            int value = toInt(row.get("value"));
            if (monthIndex >= 0 && monthIndex < 12 && weekIndex >= 0 && weekIndex < 7) {
                bucket[monthIndex][weekIndex] += value;
            }
        }

        for (Map<String, Object> row : tradeQueryService.countVoucherContributionByMonthWeek(start.atDay(1).atStartOfDay())) {
            String month = String.valueOf(row.get("month"));
            int monthIndex = parseMonthIndex(month, start);
            int weekIndex = toInt(row.get("weekIndex"));
            if (monthIndex >= 0 && monthIndex < 12 && weekIndex >= 0 && weekIndex < 7) {
                bucket[monthIndex][weekIndex] += toInt(row.get("value"));
            }
        }

        List<List<Integer>> data = new ArrayList<>();
        for (int monthIndex = 0; monthIndex < 12; monthIndex++) {
            for (int weekIndex = 0; weekIndex < 7; weekIndex++) {
                data.add(List.of(monthIndex, weekIndex, bucket[monthIndex][weekIndex]));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("months", months);
        result.put("weeks", weeks);
        result.put("data", data);
        return result;
    }

    private List<Map<String, Object>> toPieData(List<Map<String, Object>> rows) {
        String[] colors = {"#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de", "#3ba272", "#fc8452"};
        long total = rows.stream().mapToLong(row -> toLong(row.get("value"))).sum();
        if (total <= 0) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> row : rows) {
            long value = toLong(row.get("value"));
            if (value <= 0) continue;
            double percent = Math.round(value * 10000.0 / total) / 100.0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("name"));
            item.put("value", value);
            item.put("percent", percent);
            item.put("color", colors[index++ % colors.length]);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> toDashboardStatsMap(DashboardStatsVO stats) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", stats.getTotalUsers());
        result.put("totalPosts", stats.getTotalPosts());
        result.put("totalMerchants", stats.getTotalMerchants());
        result.put("totalVouchers", stats.getTotalVouchers());
        result.put("pendingStudentAuth", stats.getPendingStudentAuth());
        result.put("pendingMerchantAuth", stats.getPendingMerchantAuth());
        result.put("pendingReports", stats.getPendingReports());
        result.put("pendingRefunds", stats.getPendingRefunds());
        return result;
    }

    private int parseMonthIndex(String month, YearMonth start) {
        try {
            YearMonth value = YearMonth.parse(month);
            return (value.getYear() - start.getYear()) * 12 + value.getMonthValue() - start.getMonthValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private int toInt(Object value) {
        return (int) toLong(value);
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
