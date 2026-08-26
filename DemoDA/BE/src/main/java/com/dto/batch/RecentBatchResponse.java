package com.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentBatchResponse {

    private Long batchId;
    private String productName;
    private String merkleRoot;
    private String chainTxHash;
    private LocalDateTime createdAt;
    private String status;
}