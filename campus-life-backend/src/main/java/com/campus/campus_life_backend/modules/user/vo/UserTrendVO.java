package com.campus.campus_life_backend.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserTrendVO {
    private String day;
    private long value;
}
