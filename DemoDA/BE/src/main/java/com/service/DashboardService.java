package com.service;


import com.dto.dashboard.*;
import com.dto.batch.RecentBatchResponse;
import com.dto.record.RecentRecordResponse;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getSummary();
    List<RecentBatchResponse> getRecentBatches();
    BlockchainStatusResponse getBlockchainStatus();
    StorageCostResponse getStorageCost();
    List<UsersByRoleResponse> getUsersByRole();
    List<BatchesByDateResponse> getBatchesByDate();
    List<RecentRecordResponse> getRecentRecords();
    VerifyStatsResponse getVerifyStats();
    List<BatchStatusResponse> getAnchoredBatches();

    List<BatchStatusResponse> getPendingBatches();

    List<RecordsByBatchResponse> getRecordsByBatch();
    List<ProductsByOriginResponse> getProductsByOrigin();
}