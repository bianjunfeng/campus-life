package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;

import java.util.List;

public interface OpsLogClient extends OpsDataSource {

    List<OpsSnapshotDTO.LogTopic> listTopics();

    List<OpsSnapshotDTO.LogEntry> queryLogs(String region, String topic, String query, int limit);
}
