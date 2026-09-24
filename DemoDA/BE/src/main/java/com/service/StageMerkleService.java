package com.service;

import com.entity.Batch;
import com.enums.RecordStage;

public interface StageMerkleService {

    String refreshStageRoot(
            Batch batch,
            RecordStage recordStage
    );
}