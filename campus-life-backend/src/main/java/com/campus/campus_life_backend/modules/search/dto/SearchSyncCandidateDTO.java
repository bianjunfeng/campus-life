package com.campus.campus_life_backend.modules.search.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchSyncCandidateDTO {
    private Long id;
    private LocalDateTime updateTime;
}
