package com.campus.campus_life_backend.modules.search.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchPostItemDTO {
    private Long id;
    private String title;
    private String content;
    private String highlightTitle;
    private String highlightContent;
    private String authorName;
    private String coverImage;
    private LocalDateTime createTime;
    private Integer likeCount;
    private Integer commentCount;
}
