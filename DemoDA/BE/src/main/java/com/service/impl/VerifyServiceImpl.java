package com.service.impl;

import com.dto.verify.VerifyAllResponse;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.entity.Batch;
import com.entity.Record;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.service.DataEncryptionService;
import com.service.IpfsService;
import com.service.MerkleService;
import com.service.VerifyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VerifyServiceImpl implements VerifyService {

    private static final int AES_GCM_VERSION = 1;

    private final RecordRepository recordRepository;
    private final MerkleService merkleService;
    private final BatchRepository batchRepository;
    private final IpfsService ipfsService;
    private final DataEncryptionService dataEncryptionService;

    public VerifyServiceImpl(
            RecordRepository recordRepository,
            MerkleService merkleService,
            BatchRepository batchRepository,
            IpfsService ipfsService,
            DataEncryptionService dataEncryptionService
    ) {
        this.recordRepository = recordRepository;
        this.merkleService = merkleService;
        this.batchRepository = batchRepository;
        this.ipfsService = ipfsService;
        this.dataEncryptionService = dataEncryptionService;
    }

    @Override
    public VerifyResponse verify(VerifyRequest request) {
        if (request == null) {
            return invalidResponse(
                    null,
                    null,
                    "Request must not be null"
            );
        }

        if (request.getBatchId() == null) {
            return invalidResponse(
                    null,
                    null,
                    "batchId must not be null"
            );
        }

        /*
         * Nếu caller không truyền plaintext nhưng có recordKey,
         * hệ thống tự tải nội dung từ IPFS.
         *
         * Public record: đọc JSON trực tiếp.
         * Private record: tải ciphertext rồi giải mã AES-GCM.
         */
        String verificationRawJson = request.getRawJson();

        if (verificationRawJson == null
                || verificationRawJson.isBlank()
                || isPrivatePlaceholder(verificationRawJson)) {

            if (request.getRecordKey() == null
                    || request.getRecordKey().isBlank()) {
                return invalidResponse(
                        request.getBatchId(),
                        null,
                        "recordKey is required when rawJson is not provided"
                );
            }

            Record requestedRecord = recordRepository
                    .findByBatchIdAndRecordKey(
                            request.getBatchId(),
                            request.getRecordKey()
                    )
                    .orElse(null);

            if (requestedRecord == null) {
                return invalidResponse(
                        request.getBatchId(),
                        request.getRecordKey(),
                        "Record does not exist in this batch."
                );
            }

            try {
                verificationRawJson =
                        loadVerificationJson(requestedRecord);
            } catch (Exception e) {
                return invalidResponse(
                        request.getBatchId(),
                        request.getRecordKey(),
                        "Cannot load or decrypt record data: "
                                + e.getMessage()
                );
            }
        }

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         *
         * Hỗ trợ đồng thời:
         * V1 = Keccak-256(canonicalJson)
         * V2 = Keccak-256(saltBytes || canonicalJsonBytes)
         */
        List<Record> candidateRecords =
                recordRepository.findByBatchIdOrderByLeafIndexAsc(
                        request.getBatchId()
                );

        Record targetRecord = null;

        for (Record candidate : candidateRecords) {
            if (request.getRecordKey() != null
                    && !request.getRecordKey().isBlank()
                    && !request.getRecordKey()
                    .equals(candidate.getRecordKey())) {
                continue;
            }

            try {
                String calculatedLeafHash =
                        calculateLeafHash(
                                verificationRawJson,
                                candidate
                        );

                if (calculatedLeafHash.equalsIgnoreCase(
                        candidate.getLeafHash()
                )) {
                    targetRecord = candidate;
                    break;
                }
            } catch (Exception ignored) {
                // Candidate không hợp lệ thì bỏ qua.
            }
        }

        if (targetRecord == null) {
            return invalidResponse(
                    request.getBatchId(),
                    request.getRecordKey(),
                    "Data has been altered or does not exist in this batch."
            );
        }

        Batch batch = targetRecord.getBatch();

        if (batch == null) {
            return invalidResponse(
                    request.getBatchId(),
                    targetRecord.getRecordKey(),
                    "Record exists but does not belong to any batch."
            );
        }

        if (targetRecord.getLeafHash() == null
                || targetRecord.getLeafHash().isBlank()) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Record leafHash is missing."
            );
        }

        if (targetRecord.getLeafIndex() == null) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Record leafIndex is missing."
            );
        }

        if (batch.getMerkleRoot() == null
                || batch.getMerkleRoot().isBlank()) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Batch merkleRoot is missing."
            );
        }

        List<Record> batchRecords =
                recordRepository.findByBatchIdOrderByLeafIndexAsc(
                        batch.getId()
                );

        if (batchRecords.isEmpty()) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Batch has no records."
            );
        }

        List<String> leafHashes = batchRecords.stream()
                .map(Record::getLeafHash)
                .collect(Collectors.toList());

        List<String> proof;
        boolean valid;

        try {
            proof = merkleService.buildMerkleProof(
                    leafHashes,
                    targetRecord.getLeafIndex()
            );

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
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found with id: " + batchId
                        )
                );

        List<Record> records =
                recordRepository.findByBatchIdOrderByLeafIndexAsc(batchId);

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

        /*
         * Không truyền rawJson từ PostgreSQL vì private record
         * chỉ chứa placeholder. verify() sẽ tự lấy dữ liệu từ IPFS.
         */
        List<VerifyResponse> results = records.stream()
                .map(record -> {
                    VerifyRequest verifyRequest =
                            new VerifyRequest();

                    verifyRequest.setBatchId(batchId);
                    verifyRequest.setRecordKey(
                            record.getRecordKey()
                    );

                    return verify(verifyRequest);
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
                .invalidRecords(
                        results.size() - (int) validCount
                )
                .results(results)
                .build();
    }

    public VerifyResponse verifyRecord(
            Long batchId,
            String recordKey
    ) {
        VerifyRequest request = new VerifyRequest();
        request.setBatchId(batchId);
        request.setRecordKey(recordKey);

        // verify() tự tải và giải mã nội dung khi cần.
        return verify(request);
    }

    /*
     * Public record lấy rawJson trong PostgreSQL.
     * Private record lấy AES-GCM envelope từ IPFS và giải mã.
     */
    private String loadVerificationJson(Record record) {
        if (!Boolean.TRUE.equals(record.getEncrypted())) {
            return record.getRawJson();
        }

        if (record.getEncryptionVersion() == null
                || record.getEncryptionVersion() != AES_GCM_VERSION) {
            throw new IllegalStateException(
                    "Unsupported encryption version"
            );
        }

        if (record.getIpfsCid() == null
                || record.getIpfsCid().isBlank()) {
            throw new IllegalStateException(
                    "IPFS CID is missing"
            );
        }

        String encryptedEnvelope =
                ipfsService.getJson(record.getIpfsCid());

        String context = buildEncryptionContext(record);

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * AES-GCM kiểm tra authentication tag và AAD.
         * Sai khóa, sai context hoặc ciphertext bị sửa đều thất bại.
         */
        return dataEncryptionService.decrypt(
                encryptedEnvelope,
                context
        );
    }

    private String calculateLeafHash(
            String rawJson,
            Record record
    ) {
        Integer hashVersion = record.getHashVersion();

        // Record V1 của Giai đoạn 1.
        if (hashVersion == null || hashVersion < 2) {
            return merkleService.hashRecord(rawJson);
        }

        if (record.getLeafSalt() == null
                || record.getLeafSalt().isBlank()) {
            throw new IllegalStateException(
                    "Salt is missing for V2 record: "
                            + record.getRecordKey()
            );
        }

        // Record V2 có salted leaf.
        return merkleService.hashRecord(
                rawJson,
                record.getLeafSalt()
        );
    }

    private String buildEncryptionContext(Record record) {
        return "batch:" + record.getBatch().getId()
                + "|record:" + record.getRecordKey();
    }

    private boolean isPrivatePlaceholder(String rawJson) {
        return rawJson.contains("\"privateData\":true")
                && rawJson.contains("\"encrypted\":true");
    }

    private VerifyResponse invalidResponse(
            Long batchId,
            String recordKey,
            String message
    ) {
        return VerifyResponse.builder()
                .valid(false)
                .message(message)
                .batchId(batchId)
                .recordKey(recordKey)
                .verifiedAt(LocalDateTime.now())
                .build();
    }
}