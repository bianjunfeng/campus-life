package com.campus.campus_life_backend.modules.forum.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostTrendVO {
    private String day;
    private long value;
}
