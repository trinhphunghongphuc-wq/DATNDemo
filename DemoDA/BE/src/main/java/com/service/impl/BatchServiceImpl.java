package com.service.impl;

import com.dto.batch.*;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.entity.Batch;
import com.entity.ProductCategory;
import com.entity.Record;
import com.enums.*;
import com.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.ProductCategoryRepository;
import com.repository.RecordRepository;
import com.security.CustomUserDetails;
import com.service.BatchService;
import com.service.MerkleService;
import com.service.RecordService;
import org.springframework.stereotype.Service;
import com.dto.product.ProductCategoryResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import com.dto.product.ProductCategoryResponse;
import com.service.VerifyService;
import com.dto.verify.MerkleProofResponse;
import com.repository.StageAnchorTransactionRepository;


@Service
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final RecordService recordService;
    private final MerkleService merkleService;
    private final BlockchainConnectionService blockchainConnectionService;
    private final RecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final ProductCategoryRepository productCategoryRepository;
    private final VerifyService verifyService;
    private final StageAnchorTransactionRepository stageAnchorTransactionRepository;
    private final NotificationService notificationService;


    public BatchServiceImpl(BatchRepository batchRepository,
                            RecordService recordService,
                            MerkleService merkleService,
                            BlockchainConnectionService blockchainConnectionService, RecordRepository recordRepository, ObjectMapper objectMapper, ProductCategoryRepository productCategoryRepository, VerifyService verifyService, StageAnchorTransactionRepository stageAnchorTransactionRepository, NotificationService notificationService) {
        this.batchRepository = batchRepository;
        this.recordService = recordService;
        this.merkleService = merkleService;
        this.blockchainConnectionService = blockchainConnectionService;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.productCategoryRepository = productCategoryRepository;
        this.verifyService = verifyService;
        this.stageAnchorTransactionRepository = stageAnchorTransactionRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public BatchResponse createBatch(BatchRequest request, Role role) {
        Batch savedBatch = createBatchEntity(request, role);

        String qrContent = "http://localhost:5173/trace/batch/"
                + savedBatch.getBatchCode();

        return new BatchResponse(
                savedBatch.getId(),
                savedBatch.getName(),
                savedBatch.getBatchCode(),
                savedBatch.getMerkleRoot(),
                savedBatch.getChainTxHash(),
                qrContent
        );
    }

    @Override
    @Transactional
    public Batch createBatchEntity(BatchRequest request, Role role) {

        boolean hasDistributor = request.getDistributorId() != null;
        boolean hasRetailer = request.getRetailerId() != null;

        if (hasDistributor != hasRetailer) {
            throw new RuntimeException(
                    "Both distributorId and retailerId are required when assigning delivery"
            );
        }
        ProductCategory productCategory = null;

        if (request.getProductCategoryId() != null) {
            productCategory = productCategoryRepository.findById(request.getProductCategoryId())
                    .orElseThrow(() -> new RuntimeException("Product category not found"));
        }

        BatchStatus initialStatus = hasDistributor
                ? BatchStatus.ASSIGNED_TO_DISTRIBUTOR
                : BatchStatus.CREATED;

        Batch batch = Batch.builder()
                .name(request.getName())
                .batchCode(generateBatchCode())
                .status(initialStatus)
                .anchorStatus(AnchorStatus.NOT_ANCHORED)
                .expiryDate(request.getExpiryDate())
                .distributorId(request.getDistributorId())
                .totalWeight(request.getTotalWeight())
                .remainingWeight(request.getTotalWeight())
                .retailerId(request.getRetailerId())
                .productCategory(productCategory)
                .build();

        Batch savedBatch = batchRepository.save(batch);


        // BUILD ALL RECORDS


        List<RecordRequest> allRequests = new ArrayList<>();

        //Producer storage requirement
        if (request.getStorageRequirement() != null) {

            try {

                RecordRequest storageRecord = new RecordRequest();

                storageRecord.setRecordType(RecordType.PRODUCTION);

                storageRecord.setRawJson(
                        objectMapper.writeValueAsString(
                                request.getStorageRequirement()
                        )
                );

                allRequests.add(storageRecord);

            } catch (Exception e) {
                throw new RuntimeException("Cannot serialize storage requirement");
            }
        }

        // Other records
        if (request.getRecords() != null && !request.getRecords().isEmpty()) {
            allRequests.addAll(request.getRecords());
        }


        // SAVE RECORDS
        List<Record> savedRecords = recordService.createRecordsForBatch(
                savedBatch,
                allRequests,
                role
        );


        // BUILD MERKLE ROOT
        List<String> leafHashes = savedRecords.stream()
                .sorted(Comparator.comparingInt(Record::getLeafIndex))
                .map(Record::getLeafHash)
                .toList();

        savedBatch.setMerkleRoot(
                merkleService.buildMerkleRoot(leafHashes)
        );

        Batch result = batchRepository.save(savedBatch);
        notificationService.batchAssigned(result);
        return result;
    }



    @Override
    public List<BatchListResponse> getAllBatches() {
        List<Batch> batches = batchRepository.findAll();

        return batches.stream()
                .map(batch -> BatchListResponse.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .batchCode(batch.getBatchCode())
                        .merkleRoot(batch.getMerkleRoot())
                        .chainTxHash(batch.getChainTxHash())
                        .status(batch.getStatus())
                        .anchorStatus(batch.getAnchorStatus())
                        .recordCount(recordRepository.countByBatchId(batch.getId()))
                        .createdAt(batch.getCreatedAt())
                        .build())
                .toList();
    }


    @Override
    public Batch getBatchById(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProofResponse getProof(
            Long batchId,
            String recordKey
    ) {
        // Dùng chung logic chọn cây theo stage với API verify mới.
        MerkleProofResponse stageProof =
                verifyService.generateMerkleProof(
                        batchId,
                        recordKey
                );

        String chainTxHash;

        if (stageProof.getRecordStage() == null) {
            // Tương thích record cũ dùng cây toàn batch.
            Batch batch = getBatchById(batchId);
            chainTxHash = batch.getChainTxHash();
        } else {
            // Lấy transaction của đúng stage, không lấy transaction
            // cuối cùng của cả batch.
            chainTxHash = stageAnchorTransactionRepository
                    .findByBatch_IdAndRecordStage(
                            batchId,
                            stageProof.getRecordStage()
                    )
                    .map(anchor -> anchor.getTransactionHash())
                    .orElse(null);
        }

        ProofResponse response = new ProofResponse();

        response.setBatchId(stageProof.getBatchId());
        response.setRecordKey(stageProof.getRecordKey());
        response.setLeafHash(stageProof.getLeafHash());
        response.setProof(stageProof.getProof());
        response.setChainTxHash(chainTxHash);

        response.setRecordStage(stageProof.getRecordStage());
        response.setStageLeafIndex(stageProof.getStageLeafIndex());
        response.setStageRoot(stageProof.getStageRoot());
        response.setValid(stageProof.isValid());


        response.setLeafIndex(stageProof.getStageLeafIndex());
        response.setMerkleRoot(stageProof.getStageRoot());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyRecordProof(
            Long batchId,
            String recordKey
    ) {
        MerkleProofResponse proof =
                verifyService.generateMerkleProof(
                        batchId,
                        recordKey
                );

        return proof.isValid();
    }
    private RecordItemResponse mapToRecordItem(Record record) {
        return new RecordItemResponse(
                record.getId(),
                record.getRecordKey(),
                record.getRawJson(),
                record.getLeafHash(),
                record.getLeafIndex(),
                record.getCreatedAt(),
                record.getRecordType()
        );
    }

    private ProductCategoryResponse toProductCategoryResponse(ProductCategory category) {
        if (category == null) {
            return null;
        }

        return ProductCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .temperatureMin(category.getTemperatureMin())
                .temperatureMax(category.getTemperatureMax())
                .humidityMin(category.getHumidityMin())
                .humidityMax(category.getHumidityMax())
                .description(category.getDescription())
                .build();
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));

        List<Record> records = recordRepository.findByBatchIdOrderByLeafIndexAsc(batch.getId());

        List<RecordItemResponse> recordItems = records.stream()
                .map(this::mapToRecordItem)
                .toList();

        return BatchDetailResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .batchCode(batch.getBatchCode())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .status(batch.getStatus())
                .anchorStatus(batch.getAnchorStatus())
                .createdAt(batch.getCreatedAt())
                .recordCount(recordItems.size())
                .records(recordItems)
                .productCategory(toProductCategoryResponse(batch.getProductCategory()))
                .build();
    }

    @Override
    public BatchDetailResponse getBatchByCode(String batchCode) {
        Batch batch = batchRepository.findByBatchCode(batchCode)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with code: " + batchCode));

        List<Record> records = recordRepository.findByBatchIdOrderByLeafIndexAsc(batch.getId());

        List<RecordItemResponse> recordItems = records.stream()
                .map(this::mapToRecordItem)
                .toList();

        return BatchDetailResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .batchCode(batch.getBatchCode())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .status(batch.getStatus())
                .anchorStatus(batch.getAnchorStatus())
                .createdAt(batch.getCreatedAt())
                .recordCount(recordItems.size())
                .records(recordItems)
                .productCategory(toProductCategoryResponse(batch.getProductCategory()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchListResponse> getBatchesForUser(
            CustomUserDetails user
    ) {
        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException(
                    "Bạn không có quyền xem danh sách lô hàng."
            );
        }

        List<Batch> batches = switch (user.getRole()) {
            case ADMIN ->
                    batchRepository.findAll();

            case PRODUCER ->
                    batchRepository
                            .findByCreatedByUsernameOrderByCreatedAtDesc(
                                    user.getUsername()
                            );

            case DISTRIBUTOR ->
                    batchRepository.findByDistributorId(user.getId());

            case RETAILER ->
                    batchRepository.findByRetailerId(user.getId());

            case CONSUMER ->
                    throw new AccessDeniedException(
                            "Bạn không có quyền xem danh sách lô hàng nội bộ."
                    );
        };

        return batches.stream()
                .map(batch -> BatchListResponse.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .batchCode(batch.getBatchCode())
                        .merkleRoot(batch.getMerkleRoot())
                        .chainTxHash(batch.getChainTxHash())
                        .status(batch.getStatus())
                        .anchorStatus(batch.getAnchorStatus())
                        .recordCount(
                                recordRepository.countByBatchId(batch.getId())
                        )
                        .createdAt(batch.getCreatedAt())
                        .build())
                .toList();
    }

    private String generateBatchCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMMyyyy"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BATCH-" + datePart + "-" + randomPart;
    }
}
