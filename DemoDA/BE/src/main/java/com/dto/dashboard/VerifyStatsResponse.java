package com.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyStatsResponse {

    private long totalVerifiedRecords;
    private long totalFailedVerifications;
    private long verifiedBatches;
    private long unverifiedBatches;
}