package com.campus.campus_life_backend.modules.admin.vo;

import lombok.Data;

@Data
public class DashboardStatsVO {
    private long totalUsers;
    private long totalPosts;
    private long totalMerchants;
    private long totalVouchers;
    private long pendingStudentAuth;
    private long pendingMerchantAuth;
    private long pendingReports;
    private long pendingRefunds;
}
