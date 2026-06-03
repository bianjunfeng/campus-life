package com.campus.campus_life_backend.modules.admin.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TodayStatsVO extends DashboardStatsVO {
    private long newUsersToday;
    private long newPostsToday;
    private long newOrdersToday;
    private long newMerchantsToday;
}
