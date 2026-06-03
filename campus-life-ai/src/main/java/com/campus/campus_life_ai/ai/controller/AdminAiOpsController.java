package com.campus.campus_life_ai.ai.controller;

import com.campus.campus_life_ai.ai.dto.OpsAnalysisResponse;
import com.campus.campus_life_ai.ai.dto.OpsAnalyzeRequest;
import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;
import com.campus.campus_life_ai.ai.service.OpsAgentService;
import com.campus.campus_life_ai.common.result.ApiResponse;
import com.campus.campus_life_ai.common.security.CurrentUserAccessor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai/ops")
public class AdminAiOpsController {

    private final OpsAgentService opsAgentService;
    private final CurrentUserAccessor currentUserAccessor;

    public AdminAiOpsController(OpsAgentService opsAgentService, CurrentUserAccessor currentUserAccessor) {
        this.opsAgentService = opsAgentService;
        this.currentUserAccessor = currentUserAccessor;
    }

    @GetMapping("/snapshot")
    public ApiResponse<OpsSnapshotDTO> getSnapshot() {
        return ApiResponse.success(opsAgentService.buildSnapshot(new OpsAnalyzeRequest()));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<OpsSnapshotDTO.Alert>> getAlerts() {
        return ApiResponse.success(opsAgentService.queryAlerts());
    }

    @GetMapping("/log-topics")
    public ApiResponse<List<OpsSnapshotDTO.LogTopic>> getLogTopics() {
        return ApiResponse.success(opsAgentService.getLogTopics());
    }

    @GetMapping("/logs")
    public ApiResponse<List<OpsSnapshotDTO.LogEntry>> getLogs(@RequestParam(required = false) String region,
                                                              @RequestParam(required = false) String topic,
                                                              @RequestParam(required = false) String query,
                                                              @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(opsAgentService.queryLogs(region, topic, query, limit));
    }

    @PostMapping("/analyze")
    public ApiResponse<OpsAnalysisResponse> analyze(@RequestBody(required = false) OpsAnalyzeRequest request) {
        return ApiResponse.success(opsAgentService.analyze(currentUserAccessor.requireUserId(), request));
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@RequestBody(required = false) OpsAnalyzeRequest request) {
        return opsAgentService.analyzeStream(currentUserAccessor.requireUserId(), request);
    }
}
