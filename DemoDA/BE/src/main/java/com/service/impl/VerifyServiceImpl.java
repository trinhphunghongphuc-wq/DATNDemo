package com.service.impl;


import com.dto.verify.VerifyAllResponse;
import com.dto.verify.VerifyResponse;
import com.dto.verify.VerifyRequest;
import com.entity.Batch;
import com.entity.Record;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.service.MerkleService;
import com.service.VerifyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VerifyServiceImpl implements VerifyService {

    private final RecordRepository recordRepository;
    private final MerkleService merkleService;
    private final BatchRepository batchRepository;
    
    public VerifyServiceImpl(RecordRepository recordRepository, MerkleService merkleService, BatchRepository batchRepository) {
        this.recordRepository = recordRepository;
        this.merkleService = merkleService;
        this.batchRepository = batchRepository;
    }

    @Override
    public VerifyResponse verify(VerifyRequest request) {
        if (request == null) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Request must not be null")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (request.getBatchId() == null) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("batchId must not be null")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (request.getRawJson() == null || request.getRawJson().trim().isEmpty()) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("rawJson must not be empty")
                    .batchId(request.getBatchId())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        String inputLeafHash;
        try {
            inputLeafHash = merkleService.hashRecord(request.getRawJson());
        } catch (Exception e) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Invalid rawJson format")
                    .batchId(request.getBatchId())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        Optional<Record> optionalRecord =
                recordRepository.findByBatchIdAndLeafHash(request.getBatchId(), inputLeafHash);

        if (optionalRecord.isEmpty()) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Data has been altered or does not exist in this batch.")
                    .batchId(request.getBatchId())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        Record targetRecord = optionalRecord.get();
        Batch batch = targetRecord.getBatch();

        if (batch == null) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Record exists but does not belong to any batch.")
                    .batchId(request.getBatchId())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .leafHash(targetRecord.getLeafHash())
                    .leafIndex(targetRecord.getLeafIndex())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (targetRecord.getLeafHash() == null || targetRecord.getLeafHash().isBlank()) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Record leafHash is missing.")
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .leafIndex(targetRecord.getLeafIndex())
                    .anchorStatus(batch.getAnchorStatus())
                    .chainTxHash(batch.getChainTxHash())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (targetRecord.getLeafIndex() == null) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Record leafIndex is missing.")
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .leafHash(targetRecord.getLeafHash())
                    .anchorStatus(batch.getAnchorStatus())
                    .chainTxHash(batch.getChainTxHash())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (batch.getMerkleRoot() == null || batch.getMerkleRoot().isBlank()) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Batch merkleRoot is missing.")
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .leafHash(targetRecord.getLeafHash())
                    .leafIndex(targetRecord.getLeafIndex())
                    .anchorStatus(batch.getAnchorStatus())
                    .chainTxHash(batch.getChainTxHash())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        List<Record> batchRecords = recordRepository.findByBatchIdOrderByLeafIndexAsc(batch.getId());

        if (batchRecords == null || batchRecords.isEmpty()) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Batch has no records.")
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .merkleRoot(batch.getMerkleRoot())
                    .chainTxHash(batch.getChainTxHash())
                    .anchorStatus(batch.getAnchorStatus())
                    .leafHash(targetRecord.getLeafHash())
                    .leafIndex(targetRecord.getLeafIndex())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        List<String> leafHashes = batchRecords.stream()
                .map(Record::getLeafHash)
                .collect(Collectors.toList());

        List<String> proof;
        boolean valid;

        try {
            proof = merkleService.buildMerkleProof(leafHashes, targetRecord.getLeafIndex());
            valid = merkleService.verifyProof(
                    targetRecord.getLeafHash(),
                    proof,
                    batch.getMerkleRoot(),
                    targetRecord.getLeafIndex()
            );
        } catch (Exception e) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message("Verification error: " + e.getMessage())
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .merkleRoot(batch.getMerkleRoot())
                    .chainTxHash(batch.getChainTxHash())
                    .anchorStatus(batch.getAnchorStatus())
                    .leafHash(targetRecord.getLeafHash())
                    .leafIndex(targetRecord.getLeafIndex())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        return VerifyResponse.builder()
                .valid(valid)
                .message(valid
                        ? "Record is valid. Data integrity verified successfully."
                        : "Verification failed. Proof does not match Merkle root.")
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getName())
                .recordId(targetRecord.getId())
                .recordKey(targetRecord.getRecordKey())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .anchorStatus(batch.getAnchorStatus())
                .leafHash(targetRecord.getLeafHash())
                .leafIndex(targetRecord.getLeafIndex())
                .proof(proof)
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public VerifyAllResponse verifyAllRecords(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        List<Record> records = recordRepository.findByBatchIdOrderByLeafIndexAsc(batchId);

        if (records.isEmpty()) {
            return VerifyAllResponse.builder()
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .valid(false)
                    .message("Batch has no records.")
                    .totalRecords(0)
                    .validRecords(0)
                    .invalidRecords(0)
                    .build();
        }

        List<VerifyResponse> results = records.stream()
                .map(record -> {
                    VerifyRequest request = new VerifyRequest();
                    request.setBatchId(batchId);
                    request.setRecordKey(record.getRecordKey());
                    request.setRawJson(record.getRawJson());
                    return verify(request);
                })
                .toList();

        long validCount = results.stream()
                .filter(VerifyResponse::isValid)
                .count();

        return VerifyAllResponse.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getName())
                .valid(validCount == results.size())
                .message(validCount == results.size()
                        ? "All records are valid."
                        : "Some records are invalid.")
                .totalRecords(results.size())
                .validRecords((int) validCount)
                .invalidRecords(results.size() - (int) validCount)
                .results(results)
                .build();
    }

    public VerifyResponse verifyRecord(Long batchId, String recordKey) {
        VerifyRequest request = new VerifyRequest();
        request.setBatchId(batchId);
        request.setRecordKey(recordKey);
        return verify(request);
    }
}