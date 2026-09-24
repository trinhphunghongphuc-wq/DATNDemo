package com.service.impl;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.blockchain.StageAnchorResponse;
import com.dto.record.AdminRecordDetailResponse;
import com.dto.record.RecentRecordResponse;
import com.dto.record.RecordItemResponse;
import com.dto.user.admin.AdminUserResponse;
import com.dto.user.admin.UpdateUserRoleRequest;
import com.dto.user.admin.UpdateUserStatusRequest;
import com.entity.Batch;
import com.entity.Record;
import com.entity.User;
import com.enums.AnchorStatus;
import com.enums.Role;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.repository.UserRepository;
import com.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.entity.StageAnchorTransaction;
import com.repository.StageAnchorTransactionRepository;
import com.dto.blockchain.StageAnchorResponse;
import java.time.LocalDateTime;


import com.enums.RecordStage;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BatchRepository batchRepository;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final BlockchainConnectionService blockchainConnectionService;
    private final StageAnchorTransactionRepository
            stageAnchorTransactionRepository;

    @Override
    public List<AdminBatchListResponse> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(this::mapBatchToAdminResponse)
                .toList();
    }

    @Override
    public List<AdminBatchListResponse> filterBatches(String keyword, String status) {
        return batchRepository.findAll().stream()
                .map(this::mapBatchToAdminResponse)
                .filter(batch -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(batch.getName(), keyword))
                .filter(batch -> status == null || status.isBlank()
                        || batch.getStatus() != null
                        && batch.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    @Override
    public List<RecentRecordResponse> getAllRecords() {
        return recordRepository.findAll().stream()
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

    private String resolveStatus(Batch batch) {
        if (batch.getChainTxHash() != null && !batch.getChainTxHash().isBlank()) {
            return "ROOT_ANCHORED";
        }
        if (batch.getMerkleRoot() != null && !batch.getMerkleRoot().isBlank()) {
            return "OFFCHAIN_READY";
        }
        return "DRAFT";
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }




//    QUẢN LÝ USER
    @Override
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToResponse)
                .toList();
    }

    @Override
    public List<AdminUserResponse> filterUsers(String keyword, String role) {
        return userRepository.findAll().stream()
                .map(this::mapUserToResponse)
                .filter(user -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(user.getUsername(), keyword))
                .filter(user -> role == null || role.isBlank()
                        || user.getRole().equalsIgnoreCase(role))
                .toList();
    }

    @Override
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        return mapUserToResponse(user);
    }

    @Override
    public AdminUserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        userRepository.save(user);

        return mapUserToResponse(user);
    }

    @Override
    public AdminUserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setEnabled(request.getEnabled());
        userRepository.save(user);

        return mapUserToResponse(user);
    }

    @Override
    public AdminRecordDetailResponse getRecordById(Long id) {
        Record record = recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found with id: " + id));

        return mapRecordToDetail(record);
    }

    @Override
    public List<AdminRecordDetailResponse> filterRecords(Long batchId, String keyword) {
        return recordRepository.findAll().stream()
                .filter(record -> batchId == null || (record.getBatch() != null && record.getBatch().getId().equals(batchId)))
                .filter(record -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(record.getRecordKey(), keyword)
                        || containsIgnoreCase(record.getRawJson(), keyword))
                .map(this::mapRecordToDetail)
                .toList();
    }



    @Override
    @Transactional
    public AdminBatchListResponse anchorBatchStageRoot(
            Long batchId,
            RecordStage stage
    ) {
        if (stage == null) {
            throw new IllegalArgumentException(
                    "Record stage is required"
            );
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found with id: "
                                        + batchId
                        )
                );

        /*
         * Kiểm tra PostgreSQL trước khi gửi transaction,
         * tránh tốn gas cho một stage đã được anchor.
         */
        boolean alreadyAnchored =
                stageAnchorTransactionRepository
                        .existsByBatch_IdAndRecordStage(
                                batchId,
                                stage
                        );

        if (alreadyAnchored) {
            throw new IllegalStateException(
                    stage + " root is already anchored"
            );
        }

        String stageRoot = resolveStageRoot(
                batch,
                stage
        );

        if (stageRoot == null || stageRoot.isBlank()) {
            throw new IllegalStateException(
                    stage
                            + " Merkle root has not been generated"
            );
        }

        try {
            TransactionReceipt receipt =
                    blockchainConnectionService
                            .anchorStageRoot(
                                    batch.getId(),
                                    stage,
                                    stageRoot
                            );

            if (!receipt.isStatusOK()) {
                throw new IllegalStateException(
                        "Blockchain transaction failed"
                );
            }

            /*
             * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
             * Lưu bằng chứng anchor riêng cho từng stage.
             *
             * Các số liệu transaction hash, block number và gas used
             * sẽ được dùng trong phần đánh giá chi phí.
             */
            StageAnchorTransaction anchorTransaction =
                    StageAnchorTransaction.builder()
                            .batch(batch)
                            .recordStage(stage)
                            .transactionHash(
                                    receipt.getTransactionHash()
                            )
                            .blockNumber(
                                    receipt.getBlockNumber()
                            )
                            .gasUsed(
                                    receipt.getGasUsed()
                            )
                            .anchoredAt(
                                    LocalDateTime.now()
                            )
                            .build();

            stageAnchorTransactionRepository.save(
                    anchorTransaction
            );

            /*
             * Giữ lại dữ liệu legacy để giao diện cũ
             * vẫn đọc được transaction gần nhất.
             */
            batch.setChainTxHash(
                    receipt.getTransactionHash()
            );

            batch.setAnchorStatus(
                    AnchorStatus.ANCHORED
            );

            batchRepository.save(batch);

            return mapBatchToAdminResponse(batch);

        } catch (Exception exception) {
            batch.setAnchorStatus(
                    AnchorStatus.ANCHOR_FAILED
            );

            batchRepository.save(batch);

            throw new RuntimeException(
                    "Failed to anchor "
                            + stage
                            + " Merkle root: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String resolveStageRoot(
            Batch batch,
            RecordStage stage
    ) {
        return switch (stage) {
            case PRODUCER ->
                    batch.getProducerMerkleRoot();

            case DISTRIBUTOR ->
                    batch.getDistributorMerkleRoot();

            case RETAILER ->
                    batch.getRetailerMerkleRoot();
        };
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long batchId) {

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        return toBatchDetailResponse(batch);
    }

    @Override
    public List<StageAnchorResponse> getStageAnchors(
            Long batchId
    ) {
        if (!batchRepository.existsById(batchId)) {
            throw new RuntimeException(
                    "Batch not found with id: " + batchId
            );
        }

        return stageAnchorTransactionRepository
                .findByBatch_IdOrderByRecordStageAsc(batchId)
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                anchor ->
                                        anchor.getRecordStage()
                                                .ordinal()
                        )
                )
                .map(anchor ->
                        StageAnchorResponse.builder()
                                .id(anchor.getId())
                                .batchId(anchor.getBatch().getId())
                                .recordStage(anchor.getRecordStage())
                                .transactionHash(
                                        anchor.getTransactionHash()
                                )
                                .blockNumber(anchor.getBlockNumber())
                                .gasUsed(anchor.getGasUsed())
                                .anchoredAt(anchor.getAnchoredAt())
                                .build()
                )
                .toList();
    }


    private BatchDetailResponse toBatchDetailResponse(Batch batch) {
        return BatchDetailResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .batchCode(batch.getBatchCode())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .status(batch.getStatus())
                .anchorStatus(batch.getAnchorStatus())
                .createdAt(batch.getCreatedAt())
                .recordCount(batch.getRecords() == null ? 0 : batch.getRecords().size())
                .records(
                        batch.getRecords()
                                .stream()
                                .map(record -> RecordItemResponse.builder()
                                        .recordId(record.getId())
                                        .recordKey(record.getRecordKey())
                                        .recordType(record.getRecordType())
                                        .rawJson(record.getRawJson())
                                        .leafHash(record.getLeafHash())
                                        .leafIndex(record.getLeafIndex())
                                        .createdAt(record.getCreatedAt())
                                        .build())
                                .toList()
                )
                .build();
    }


    private AdminBatchListResponse mapBatchToAdminResponse(Batch batch) {
        int recordCount = recordRepository.findByBatchId(batch.getId()).size();

        return new AdminBatchListResponse(
                batch.getId(),
                batch.getName(),
                batch.getMerkleRoot(),
                batch.getChainTxHash(),
                batch.getCreatedAt(),
                batch.getStatus() != null ? batch.getStatus().name() : null,
                recordCount,
                batch.getAnchorStatus() != null ? batch.getAnchorStatus().name() : null
        );
    }

    private AdminRecordDetailResponse mapRecordToDetail(Record record) {
        return new AdminRecordDetailResponse(
                record.getId(),
                record.getRecordKey(),
                record.getBatch() != null ? record.getBatch().getId() : null,
                record.getBatch() != null ? record.getBatch().getName() : null,
                record.getRawJson(),
                record.getLeafHash(),
                record.getLeafIndex(),
                record.getCreatedAt()
        );
    }


    private AdminUserResponse mapUserToResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getEnabled(),
                user.getCreatedAt()
        );
    }
}