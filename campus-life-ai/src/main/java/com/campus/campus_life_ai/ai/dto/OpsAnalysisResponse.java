package com.campus.campus_life_ai.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OpsAnalysisResponse {
    private String report;
    private OpsSnapshotDTO snapshot;
    private String providerCode;
    private String modelCode;
    private Integer latencyMs;
    private LocalDateTime generatedAt;
}
