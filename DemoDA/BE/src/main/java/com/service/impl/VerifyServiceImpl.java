package com.service.impl;

import com.dto.verify.MerkleProofResponse;
import com.dto.verify.VerifyAllResponse;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.entity.Batch;
import com.entity.Record;
import com.enums.RecordStage;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.service.DataEncryptionService;
import com.service.IpfsService;
import com.service.MerkleService;
import com.service.VerifyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
                    request.getRecordKey(),
                    "batchId must not be null"
            );
        }

        String verificationRawJson = request.getRawJson();

        /*
         * Nếu không truyền plaintext, hệ thống tự lấy dữ liệu:
         *
         * Public record: lấy rawJson từ PostgreSQL.
         * Private record: lấy ciphertext từ IPFS và giải mã AES-GCM.
         */
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
         * Hỗ trợ đồng thời:
         *
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

                if (candidate.getLeafHash() != null
                        && calculatedLeafHash.equalsIgnoreCase(
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

        MerkleVerificationContext merkleContext;

        try {
            merkleContext = resolveMerkleContext(
                    batch,
                    targetRecord
            );
        } catch (Exception e) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Cannot resolve Merkle context: "
                            + e.getMessage()
            );
        }

        if (merkleContext.expectedRoot() == null
                || merkleContext.expectedRoot().isBlank()) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Merkle root is missing for record stage."
            );
        }

        if (merkleContext.records().isEmpty()) {
            return invalidResponse(
                    batch.getId(),
                    targetRecord.getRecordKey(),
                    "Merkle stage has no records."
            );
        }

        List<String> leafHashes = merkleContext.records()
                .stream()
                .map(Record::getLeafHash)
                .toList();

        List<String> proof;
        boolean valid;

        try {
            /*
             * Proof được tạo tại thời điểm yêu cầu.
             * Không lưu sẵn proof trong PostgreSQL hoặc IPFS.
             */
            proof = merkleService.buildMerkleProof(
                    leafHashes,
                    merkleContext.proofIndex()
            );

            valid = merkleService.verifyProof(
                    targetRecord.getLeafHash(),
                    proof,
                    merkleContext.expectedRoot(),
                    merkleContext.proofIndex()
            );
        } catch (Exception e) {
            return VerifyResponse.builder()
                    .valid(false)
                    .message(
                            "Verification error: "
                                    + e.getMessage()
                    )
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .recordId(targetRecord.getId())
                    .recordKey(targetRecord.getRecordKey())
                    .merkleRoot(merkleContext.expectedRoot())
                    .chainTxHash(batch.getChainTxHash())
                    .anchorStatus(batch.getAnchorStatus())
                    .leafHash(targetRecord.getLeafHash())

                    /*
                     * leafIndex trong VerifyResponse là index thực tế
                     * dùng để tạo và xác minh proof.
                     */
                    .leafIndex(merkleContext.proofIndex())
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        return VerifyResponse.builder()
                .valid(valid)
                .message(valid
                        ? "Record is valid. Data integrity verified successfully."
                        : "Verification failed. Proof does not match stage Merkle root.")
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getName())
                .recordId(targetRecord.getId())
                .recordKey(targetRecord.getRecordKey())
                .merkleRoot(merkleContext.expectedRoot())
                .chainTxHash(batch.getChainTxHash())
                .anchorStatus(batch.getAnchorStatus())
                .leafHash(targetRecord.getLeafHash())
                .leafIndex(merkleContext.proofIndex())
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
                recordRepository.findByBatchIdOrderByLeafIndexAsc(
                        batchId
                );

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
                    .results(List.of())
                    .build();
        }

        /*
         * Không lấy rawJson trực tiếp từ PostgreSQL.
         * verify() sẽ tự tải và giải mã dữ liệu khi cần.
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

        int validCount = (int) results.stream()
                .filter(VerifyResponse::isValid)
                .count();

        int invalidCount =
                results.size() - validCount;

        return VerifyAllResponse.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getName())
                .valid(invalidCount == 0)
                .message(invalidCount == 0
                        ? "All records are valid."
                        : "Some records are invalid.")
                .totalRecords(results.size())
                .validRecords(validCount)
                .invalidRecords(invalidCount)
                .results(results)
                .build();
    }

    @Override
    public MerkleProofResponse generateMerkleProof(
            Long batchId,
            String recordKey
    ) {
        if (batchId == null) {
            throw new IllegalArgumentException(
                    "batchId must not be null"
            );
        }

        if (recordKey == null || recordKey.isBlank()) {
            throw new IllegalArgumentException(
                    "recordKey must not be blank"
            );
        }

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found with id: "
                                        + batchId
                        )
                );

        Record targetRecord = recordRepository
                .findByBatchIdAndRecordKey(
                        batchId,
                        recordKey
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Record not found with batchId "
                                        + batchId
                                        + " and recordKey "
                                        + recordKey
                        )
                );

        if (targetRecord.getLeafHash() == null
                || targetRecord.getLeafHash().isBlank()) {
            throw new IllegalStateException(
                    "Record leafHash is missing"
            );
        }

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Xác định đúng cây Merkle theo giai đoạn của record.
         *
         * Producer, Distributor và Retailer sử dụng các cây riêng,
         * vì vậy proof của stage cũ không bị thay đổi khi stage mới
         * thêm record.
         */
        MerkleVerificationContext merkleContext =
                resolveMerkleContext(
                        batch,
                        targetRecord
                );

        if (merkleContext.expectedRoot() == null
                || merkleContext.expectedRoot().isBlank()) {
            throw new IllegalStateException(
                    "Merkle root is missing for record stage"
            );
        }

        if (merkleContext.records().isEmpty()) {
            throw new IllegalStateException(
                    "Merkle stage has no records"
            );
        }

        int proofIndex = merkleContext.proofIndex();

        if (proofIndex < 0
                || proofIndex >= merkleContext.records().size()) {
            throw new IllegalStateException(
                    "Invalid Merkle leaf index: "
                            + proofIndex
            );
        }

        List<String> leafHashes =
                merkleContext.records()
                        .stream()
                        .map(Record::getLeafHash)
                        .toList();

        if (leafHashes.stream().anyMatch(
                hash -> hash == null || hash.isBlank()
        )) {
            throw new IllegalStateException(
                    "Merkle stage contains a record without leafHash"
            );
        }

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Proof chỉ được sinh tại thời điểm API này được gọi.
         *
         * Proof không được lưu trong PostgreSQL hoặc IPFS,
         * giúp giảm dung lượng lưu trữ và tránh proof bị lỗi thời.
         */
        List<String> proof =
                merkleService.buildMerkleProof(
                        leafHashes,
                        proofIndex
                );

        /*
         * Backend tự kiểm tra proof trước khi trả cho client.
         */
        boolean valid = merkleService.verifyProof(
                targetRecord.getLeafHash(),
                proof,
                merkleContext.expectedRoot(),
                proofIndex
        );

        return MerkleProofResponse.builder()
                .batchId(batch.getId())
                .recordKey(targetRecord.getRecordKey())
                .recordStage(targetRecord.getRecordStage())
                .leafHash(targetRecord.getLeafHash())
                .stageLeafIndex(proofIndex)
                .stageRoot(merkleContext.expectedRoot())
                .proof(proof)
                .proofSize(proof.size())
                .valid(valid)
                .build();
    }

    public VerifyResponse verifyRecord(
            Long batchId,
            String recordKey
    ) {
        VerifyRequest request = new VerifyRequest();
        request.setBatchId(batchId);
        request.setRecordKey(recordKey);

        return verify(request);
    }

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     *
     * Mỗi giai đoạn có cây Merkle độc lập. Khi xác minh:
     * - Producer dùng producerMerkleRoot.
     * - Distributor dùng distributorMerkleRoot.
     * - Retailer dùng retailerMerkleRoot.
     *
     * Điều này ngăn record của stage mới làm thay đổi proof
     * của các stage đã hoàn thành.
     */
    private MerkleVerificationContext resolveMerkleContext(
            Batch batch,
            Record targetRecord
    ) {
        RecordStage stage = targetRecord.getRecordStage();
        Integer stageLeafIndex =
                targetRecord.getStageLeafIndex();

        if (stage != null && stageLeafIndex != null) {
            List<Record> stageRecords = recordRepository
                    .findByBatchIdAndRecordStageOrderByStageLeafIndexAsc(
                            batch.getId(),
                            stage
                    );

            String stageRoot = switch (stage) {
                case PRODUCER ->
                        batch.getProducerMerkleRoot();

                case DISTRIBUTOR ->
                        batch.getDistributorMerkleRoot();

                case RETAILER ->
                        batch.getRetailerMerkleRoot();
            };

            return new MerkleVerificationContext(
                    stageRecords,
                    stageLeafIndex,
                    stageRoot
            );
        }

        /*
         * Tương thích record cũ của Giai đoạn 1:
         * nếu chưa có recordStage/stageLeafIndex thì dùng cây toàn batch.
         */
        if (targetRecord.getLeafIndex() == null) {
            throw new IllegalStateException(
                    "Record leaf index is missing"
            );
        }

        List<Record> legacyRecords =
                recordRepository.findByBatchIdOrderByLeafIndexAsc(
                        batch.getId()
                );

        return new MerkleVerificationContext(
                legacyRecords,
                targetRecord.getLeafIndex(),
                batch.getMerkleRoot()
        );
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
                || record.getEncryptionVersion()
                != AES_GCM_VERSION) {
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

        String context =
                buildEncryptionContext(record);

        /*
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
        Integer hashVersion =
                record.getHashVersion();

        // Record V1 cũ, chưa sử dụng salt.
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

        return merkleService.hashRecord(
                rawJson,
                record.getLeafSalt()
        );
    }

    private String buildEncryptionContext(
            Record record
    ) {
        return "batch:" + record.getBatch().getId()
                + "|record:" + record.getRecordKey();
    }

    private boolean isPrivatePlaceholder(
            String rawJson
    ) {
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

    private record MerkleVerificationContext(
            List<Record> records,
            int proofIndex,
            String expectedRoot
    ) {
    }
}