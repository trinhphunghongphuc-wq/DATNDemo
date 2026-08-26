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
import com.service.BatchService;
import com.service.MerkleService;
import com.service.RecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final RecordService recordService;
    private final MerkleService merkleService;
    private final BlockchainConnectionService blockchainConnectionService;
    private final RecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final ProductCategoryRepository productCategoryRepository;

    public BatchServiceImpl(BatchRepository batchRepository,
                            RecordService recordService,
                            MerkleService merkleService,
                            BlockchainConnectionService blockchainConnectionService, RecordRepository recordRepository, ObjectMapper objectMapper, ProductCategoryRepository productCategoryRepository) {
        this.batchRepository = batchRepository;
        this.recordService = recordService;
        this.merkleService = merkleService;
        this.blockchainConnectionService = blockchainConnectionService;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Override
    public BatchResponse createBatch(BatchRequest request, Role role) {
        Batch savedBatch = createBatchEntity(request, role);

        return new BatchResponse(
                savedBatch.getId(),
                savedBatch.getName(),
                savedBatch.getMerkleRoot(),
                savedBatch.getChainTxHash()
        );
    }

    @Override
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

        return batchRepository.save(savedBatch);
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
    public ProofResponse getProof(Long batchId, String recordKey) {
        Batch batch = getBatchById(batchId);
        Record targetRecord = recordService.getByBatchIdAndRecordKey(batchId, recordKey);

        List<Record> records = recordService.getRecordsByBatchId(batchId);
        records.sort(Comparator.comparingInt(Record::getLeafIndex));

        List<String> leafHashes = records.stream()
                .map(Record::getLeafHash)
                .toList();

        List<String> proof = merkleService.generateProof(leafHashes, targetRecord.getLeafIndex());

        return new ProofResponse(
                batch.getId(),
                targetRecord.getRecordKey(),
                targetRecord.getLeafIndex(),
                targetRecord.getLeafHash(),
                batch.getMerkleRoot(),
                proof,
                batch.getChainTxHash()
        );
    }

    @Override
    public boolean verifyRecordProof(Long batchId, String recordKey) {
        Batch batch = getBatchById(batchId);
        Record targetRecord = recordService.getByBatchIdAndRecordKey(batchId, recordKey);

        List<Record> records = recordService.getRecordsByBatchId(batchId);
        records.sort(Comparator.comparingInt(Record::getLeafIndex));

        List<String> leafHashes = records.stream()
                .map(Record::getLeafHash)
                .toList();

        List<String> proof = merkleService.generateProof(leafHashes, targetRecord.getLeafIndex());

        return merkleService.verifyProof(
                targetRecord.getLeafHash(),
                proof,
                batch.getMerkleRoot(),
                targetRecord.getLeafIndex()
        );
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
                .build();
    }

    private String generateBatchCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMMyyyy"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BATCH-" + datePart + "-" + randomPart;
    }
}