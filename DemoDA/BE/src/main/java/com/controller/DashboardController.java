package com.controller;


import com.dto.dashboard.*;
import com.dto.batch.RecentBatchResponse;
import com.dto.record.RecentRecordResponse;
import com.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/recent-batches")
    public List<RecentBatchResponse> getRecentBatches() {
        return dashboardService.getRecentBatches();
    }

    @GetMapping("/blockchain-status")
    public BlockchainStatusResponse getBlockchainStatus() {
        return dashboardService.getBlockchainStatus();
    }

    @GetMapping("/storage-cost")
    public StorageCostResponse getStorageCost() {
        return dashboardService.getStorageCost();
    }

    @GetMapping("/users-by-role")
    public List<UsersByRoleResponse> getUsersByRole() {
        return dashboardService.getUsersByRole();
    }

    @GetMapping("/batches-by-date")
    public List<BatchesByDateResponse> getBatchesByDate() {
        return dashboardService.getBatchesByDate();
    }

    @GetMapping("/recent-records")
    public List<RecentRecordResponse> getRecentRecords() {
        return dashboardService.getRecentRecords();
    }

    @GetMapping("/verify-stats")
    public VerifyStatsResponse getVerifyStats() {
        return dashboardService.getVerifyStats();
    }

    @GetMapping("/anchored-batches")
    public List<BatchStatusResponse> getAnchoredBatches() {
        return dashboardService.getAnchoredBatches();
    }

    @GetMapping("/pending-batches")
    public List<BatchStatusResponse> getPendingBatches() {
        return dashboardService.getPendingBatches();
    }
    @GetMapping("/records-by-batch")
    public List<RecordsByBatchResponse> getRecordsByBatch() {
        return dashboardService.getRecordsByBatch();
    }
    @GetMapping("/products-by-origin")
    public List<ProductsByOriginResponse> getProductsByOrigin() {
        return dashboardService.getProductsByOrigin();
    }
}