package com.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StorageCostResponse {

    private long totalRecords;
    private long totalBatches;
    private long anchoredBatches;
    private long totalOffChainBytes;
    private long estimatedOnChainFullBytes;
    private long estimatedMerkleOnlyBytes;
    private double savingPercent;
}