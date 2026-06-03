package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;

public interface OpsDataSource {

    OpsSnapshotDTO.DataSourceStatus status();
}
