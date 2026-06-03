package com.campus.campus_life_ai.ai.service.ops;

import com.campus.campus_life_ai.ai.dto.OpsSnapshotDTO;

import java.util.List;

public interface OpsAlertClient extends OpsDataSource {

    List<OpsSnapshotDTO.Alert> queryAlerts();
}
