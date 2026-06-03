package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;

final class OpsDataSourceStatuses {

    private OpsDataSourceStatuses() {
    }

    static OpsSnapshotDTO.DataSourceStatus status(String code, String name, String status, String message) {
        OpsSnapshotDTO.DataSourceStatus dataSource = new OpsSnapshotDTO.DataSourceStatus();
        dataSource.setCode(code);
        dataSource.setName(name);
        dataSource.setStatus(status);
        dataSource.setMessage(message);
        return dataSource;
    }
}
