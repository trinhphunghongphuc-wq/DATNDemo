package com.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchStatusResponse {

    private Long batchId;
    private String batchName;
    private String merkleRoot;
    private String chainTxHash;
    private LocalDateTime createdAt;
    private String status;
}