package com.campus.campus_life_ai.ai.dto;

import lombok.Data;

@Data
public class OpsAnalyzeRequest {
    private String question;
    private Boolean includeAlerts;
    private Boolean includeLogs;
    private Boolean includeAiLogs;
    private Integer logLimit;
}
