package com.repository;

import com.entity.StageWalletApproval;
import com.enums.RecordStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageWalletApprovalRepository extends JpaRepository<StageWalletApproval, Long> {
    Optional<StageWalletApproval> findByBatchIdAndStage(Long batchId, RecordStage stage);
    List<StageWalletApproval> findByBatchId(Long batchId);
}
