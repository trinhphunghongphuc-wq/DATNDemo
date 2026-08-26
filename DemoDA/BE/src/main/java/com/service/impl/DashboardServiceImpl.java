package com.service.impl;


import com.dto.dashboard.*;
import com.dto.batch.RecentBatchResponse;
import com.dto.record.RecentRecordResponse;
import com.entity.Batch;
import com.enums.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.repository.UserRepository;
import com.entity.Record;
import com.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BatchRepository batchRepository;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final BlockchainConnectionService blockchainConnectionService;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardSummaryResponse getSummary() {

        long totalBatches = batchRepository.count();
        long totalRecords = recordRepository.count();
        long totalUsers = userRepository.count();

        long anchoredBatches = batchRepository.countByChainTxHashIsNotNull();
        long pendingBatches = batchRepository.countByChainTxHashIsNull();

        // Tạm thời cho verifiedProofs = anchoredBatches
        // Sau này nếu bạn có bảng/log verify riêng thì đổi lại
        long verifiedProofs = anchoredBatches;

        return new DashboardSummaryResponse(
                totalBatches,
                totalRecords,
                totalUsers,
                anchoredBatches,
                pendingBatches,
                verifiedProofs
        );
    }

    @Override
    public List<RecentBatchResponse> getRecentBatches() {
        List<Batch> batches = batchRepository.findTop5ByOrderByCreatedAtDesc();

        return batches.stream()
                .map(batch -> new RecentBatchResponse(
                        batch.getId(),
                        extractProductName(batch),
                        batch.getMerkleRoot(),
                        batch.getChainTxHash(),
                        batch.getCreatedAt(),
                        resolveStatus(batch)
                ))
                .toList();
    }

    @Override
    public BlockchainStatusResponse getBlockchainStatus() {
        try {
            String clientVersion = blockchainConnectionService.getClientVersion();
            Long currentBlock = blockchainConnectionService.getBlockNumber();
            String contractAddress = blockchainConnectionService.getContractAddress();
            String walletAddress = blockchainConnectionService.getWalletAddress();

            return new BlockchainStatusResponse(
                    true,
                    clientVersion,
                    currentBlock,
                    contractAddress,
                    walletAddress
            );
        } catch (Exception e) {
            return new BlockchainStatusResponse(
                    false,
                    "Disconnected",
                    0L,
                    null,
                    null
            );
        }
    }

    @Override
    public StorageCostResponse getStorageCost() {
        long totalRecords = recordRepository.count();
        long totalBatches = batchRepository.count();
        long anchoredBatches = batchRepository.countByChainTxHashIsNotNull();

        List<Record> records = recordRepository.findAll();

        long totalOffChainBytes = records.stream()
                .map(Record::getRawJson)
                .filter(rawJson -> rawJson != null)
                .mapToLong(rawJson -> rawJson.getBytes().length)
                .sum();

        long estimatedOnChainFullBytes = totalOffChainBytes;

        // Mỗi batch lưu 1 merkle root ~ 32 bytes
        long estimatedMerkleOnlyBytes = totalBatches * 32L;

        double savingPercent = 0.0;
        if (estimatedOnChainFullBytes > 0) {
            savingPercent = ((double) (estimatedOnChainFullBytes - estimatedMerkleOnlyBytes)
                    / estimatedOnChainFullBytes) * 100.0;
        }

        return new StorageCostResponse(
                totalRecords,
                totalBatches,
                anchoredBatches,
                totalOffChainBytes,
                estimatedOnChainFullBytes,
                estimatedMerkleOnlyBytes,
                Math.max(savingPercent, 0.0)
        );
    }

    @Override
    public List<UsersByRoleResponse> getUsersByRole() {
        return List.of(
                new UsersByRoleResponse("PRODUCER", userRepository.countByRole(Role.PRODUCER)),
                new UsersByRoleResponse("DISTRIBUTOR", userRepository.countByRole(Role.DISTRIBUTOR)),
                new UsersByRoleResponse("RETAILER", userRepository.countByRole(Role.RETAILER)),
                new UsersByRoleResponse("CONSUMER", userRepository.countByRole(Role.CONSUMER))
        );
    }

    @Override
    public List<BatchesByDateResponse> getBatchesByDate() {
        List<Batch> batches = batchRepository.findAll();

        Map<LocalDate, Long> grouped = batches.stream()
                .filter(batch -> batch.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        batch -> batch.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new BatchesByDateResponse(
                        entry.getKey().toString(),
                        entry.getValue()
                ))
                .toList();
    }

    @Override
    public List<RecentRecordResponse> getRecentRecords() {
        List<Record> records = recordRepository.findTop5ByOrderByCreatedAtDesc();

        return records.stream()
                .map(record -> new RecentRecordResponse(
                        record.getId(),
                        record.getRecordKey(),
                        record.getBatch() != null ? record.getBatch().getId() : null,
                        record.getBatch() != null ? record.getBatch().getName() : null,
                        record.getLeafHash(),
                        record.getLeafIndex(),
                        record.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public VerifyStatsResponse getVerifyStats() {
        long verifiedBatches = batchRepository.countByMerkleRootIsNotNull();
        long unverifiedBatches = batchRepository.countByMerkleRootIsNull();

        long totalVerifiedRecords = recordRepository.countByBatch_MerkleRootIsNotNull();

        // Hiện tại chưa có bảng log verify fail nên để 0
        long totalFailedVerifications = 0L;

        return new VerifyStatsResponse(
                totalVerifiedRecords,
                totalFailedVerifications,
                verifiedBatches,
                unverifiedBatches
        );
    }

    private String resolvePendingStatus(Batch batch) {
        if (batch.getMerkleRoot() != null && !batch.getMerkleRoot().isBlank()) {
            return "OFFCHAIN_READY";
        }
        return "DRAFT";
    }

    @Override
    public List<BatchStatusResponse> getAnchoredBatches() {
        List<Batch> batches = batchRepository.findTop10ByChainTxHashIsNotNullOrderByCreatedAtDesc();

        return batches.stream()
                .map(batch -> new BatchStatusResponse(
                        batch.getId(),
                        batch.getName(),
                        batch.getMerkleRoot(),
                        batch.getChainTxHash(),
                        batch.getCreatedAt(),
                        "ROOT_ANCHORED"
                ))
                .toList();
    }

    @Override
    public List<BatchStatusResponse> getPendingBatches() {
        List<Batch> batches = batchRepository.findTop10ByChainTxHashIsNullOrderByCreatedAtDesc();

        return batches.stream()
                .map(batch -> new BatchStatusResponse(
                        batch.getId(),
                        batch.getName(),
                        batch.getMerkleRoot(),
                        batch.getChainTxHash(),
                        batch.getCreatedAt(),
                        resolvePendingStatus(batch)
                ))
                .toList();
    }

    @Override
    public List<RecordsByBatchResponse> getRecordsByBatch() {
        List<Batch> batches = batchRepository.findAll();

        return batches.stream()
                .map(batch -> new RecordsByBatchResponse(
                        batch.getId(),
                        batch.getName(),
                        recordRepository.findByBatchId(batch.getId()).size()
                ))
                .toList();
    }

    @Override
    public List<ProductsByOriginResponse> getProductsByOrigin() {
        List<Record> records = recordRepository.findAll();

        Map<String, Long> grouped = records.stream()
                .map(this::extractOrigin)
                .filter(origin -> origin != null && !origin.isBlank())
                .collect(Collectors.groupingBy(
                        origin -> origin,
                        Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> new ProductsByOriginResponse(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }


    private String extractOrigin(Record record) {
        try {
            if (record.getRawJson() == null || record.getRawJson().isBlank()) {
                return "UNKNOWN";
            }

            Map<String, Object> rawData = objectMapper.readValue(
                    record.getRawJson(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Object origin = rawData.get("origin");
            return origin != null ? origin.toString() : "UNKNOWN";

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String extractProductName(Batch batch) {
        return batch.getName();
    }

    private String resolveStatus(Batch batch) {
        if (batch.getChainTxHash() != null && !batch.getChainTxHash().isBlank()) {
            return "ROOT_ANCHORED";
        }
        if (batch.getMerkleRoot() != null && !batch.getMerkleRoot().isBlank()) {
            return "OFFCHAIN_READY";
        }
        return "DRAFT";
    }
}