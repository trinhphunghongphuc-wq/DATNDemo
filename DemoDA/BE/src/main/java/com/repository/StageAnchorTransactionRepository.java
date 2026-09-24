package com.repository;

import com.entity.StageAnchorTransaction;
import com.enums.RecordStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageAnchorTransactionRepository
        extends JpaRepository<StageAnchorTransaction, Long> {

    boolean existsByBatch_IdAndRecordStage(
            Long batchId,
            RecordStage recordStage
    );

    Optional<StageAnchorTransaction>
    findByBatch_IdAndRecordStage(
            Long batchId,
            RecordStage recordStage
    );

    List<StageAnchorTransaction>
    findByBatch_IdOrderByRecordStageAsc(
            Long batchId
    );
}