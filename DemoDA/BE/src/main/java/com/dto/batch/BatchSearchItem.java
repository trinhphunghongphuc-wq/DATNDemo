package com.dto.batch;

import com.entity.Batch;
import com.enums.AnchorStatus;
import com.enums.BatchStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchSearchItem(Long id, String name, String batchCode, BatchStatus status,
        LocalDateTime createdAt, LocalDate expiryDate, AnchorStatus anchorStatus,
        String merkleRoot, String chainTxHash, long recordCount) {
    public static BatchSearchItem from(Batch batch, long recordCount) {
        return new BatchSearchItem(batch.getId(), batch.getName(), batch.getBatchCode(),
                batch.getStatus(), batch.getCreatedAt(), batch.getExpiryDate(), batch.getAnchorStatus(),
                batch.getMerkleRoot(), batch.getChainTxHash(), recordCount);
    }
}
