package com.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryResponse {

    private long totalBatches;
    private long totalRecords;
    private long totalUsers;
    private long anchoredBatches;
    private long pendingBatches;
    private long verifiedProofs;
}