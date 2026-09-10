package com.service.impl;

import com.dto.record.RecordRequest;
import com.entity.Batch;
import com.entity.Record;
import com.enums.BatchStatus;
import com.enums.RecordStage;
import com.enums.RecordType;
import com.enums.Role;
import com.exception.ResourceNotFoundException;
import com.repository.RecordRepository;
import com.service.DataEncryptionService;
import com.service.IpfsService;
import com.service.MerkleService;
import com.service.RecordService;
import com.service.StageMerkleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecordServiceImpl implements RecordService {

    private static final int SALTED_HASH_VERSION = 2;
    private static final int AES_GCM_VERSION = 1;

    /*
     * Nội dung thay thế được lưu trong PostgreSQL đối với record riêng tư.
     * Plaintext thật chỉ được hash trong bộ nhớ và mã hóa trước khi upload IPFS.
     */
    private static final String PRIVATE_DATA_PLACEHOLDER =
            "{\"privateData\":true,\"encrypted\":true}";

    private final RecordRepository recordRepository;
    private final MerkleService merkleService;
    private final IpfsService ipfsService;
    private final StageMerkleService stageMerkleService;
    private final DataEncryptionService dataEncryptionService;

    public RecordServiceImpl(
            RecordRepository recordRepository,
            MerkleService merkleService,
            IpfsService ipfsService,
            StageMerkleService stageMerkleService,
            DataEncryptionService dataEncryptionService
    ) {
        this.recordRepository = recordRepository;
        this.merkleService = merkleService;
        this.ipfsService = ipfsService;
        this.stageMerkleService = stageMerkleService;
        this.dataEncryptionService = dataEncryptionService;
    }

    private void validateStageCanAddRecord(
            Batch batch,
            RecordStage recordStage
    ) {
        BatchStatus status = batch.getStatus();

        boolean allowed = switch (recordStage) {
            case PRODUCER ->
                    status == BatchStatus.CREATED
                            || status == BatchStatus.IN_PRODUCTION
                            || status == BatchStatus.ASSIGNED_TO_DISTRIBUTOR;

            case DISTRIBUTOR ->
                    status == BatchStatus.RECEIVED_BY_DISTRIBUTOR
                            || status == BatchStatus.IN_DISTRIBUTION;

            case RETAILER ->
                    status == BatchStatus.AT_RETAIL;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Cannot add " + recordStage
                            + " record when batch status is "
                            + status
            );
        }
    }

    @Override
    @Transactional
    public List<Record> createRecordsForBatch(
            Batch batch,
            List<RecordRequest> requests,
            Role role
    ) {
        validateRecordWritable(batch);

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<Record> records = new ArrayList<>();

        // Vị trí record trong toàn bộ batch.
        int startIndex = recordRepository
                .findByBatchId(batch.getId())
                .size();

        // Vị trí tiếp theo trong từng cây Merkle riêng.
        Map<RecordStage, Integer> nextStageIndexes =
                new EnumMap<>(RecordStage.class);

        for (int i = 0; i < requests.size(); i++) {
            RecordRequest request = requests.get(i);

            validateRoleCanCreateRecordType(
                    role,
                    request.getRecordType()
            );

            RecordStage recordStage =
                    resolveRecordStage(request.getRecordType());

            // Chặn sai role/state trước khi tạo dữ liệu trên IPFS.
            validateStageCanAddRecord(batch, recordStage);

            String recordKey =
                    generateRecordKey(request.getRecordType());

            String originalRawJson = request.getRawJson();

            /*
             * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
             * Merkle commitment luôn được tính trên plaintext gốc
             * và dùng salt riêng cho từng record.
             *
             * Leaf V2 = Keccak-256(saltBytes || canonicalJsonBytes)
             */
            String leafSalt = merkleService.generateSalt();

            String leafHash = merkleService.hashRecord(
                    originalRawJson,
                    leafSalt
            );

            boolean privateData = request.isPrivateData();

            String ipfsPayload;
            String databaseRawJson;
            Integer encryptionVersion;

            if (privateData) {
                /*
                 * AAD ràng buộc ciphertext với đúng batch và record.
                 * Nếu ciphertext bị chuyển sang record khác,
                 * AES-GCM sẽ từ chối giải mã.
                 */
                String encryptionContext =
                        buildEncryptionContext(batch, recordKey);

                /*
                 * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
                 * Dữ liệu riêng tư được mã hóa AES-256-GCM
                 * trước khi upload lên IPFS.
                 */
                ipfsPayload = dataEncryptionService.encrypt(
                        originalRawJson,
                        encryptionContext
                );

                /*
                 * Không lưu plaintext riêng tư trong PostgreSQL.
                 * API public và trang QR chỉ nhìn thấy placeholder này.
                 */
                databaseRawJson = PRIVATE_DATA_PLACEHOLDER;
                encryptionVersion = AES_GCM_VERSION;
            } else {
                ipfsPayload = originalRawJson;
                databaseRawJson = originalRawJson;
                encryptionVersion = null;
            }

            String ipfsCid =
                    ipfsService.uploadJson(ipfsPayload);

            int stageLeafIndex = nextStageIndexes.computeIfAbsent(
                    recordStage,
                    stage -> recordRepository
                            .countByBatchIdAndRecordStage(
                                    batch.getId(),
                                    stage
                            )
            );

            nextStageIndexes.put(
                    recordStage,
                    stageLeafIndex + 1
            );

            Record record = Record.builder()
                    .recordKey(recordKey)
                    .recordType(request.getRecordType())

                    // Plaintext với public record, placeholder với private record.
                    .rawJson(databaseRawJson)

                    .ipfsCid(ipfsCid)
                    .encrypted(privateData)
                    .encryptionVersion(encryptionVersion)

                    .leafSalt(leafSalt)
                    .hashVersion(SALTED_HASH_VERSION)
                    .leafHash(leafHash)

                    .recordStage(recordStage)
                    .stageLeafIndex(stageLeafIndex)
                    .leafIndex(startIndex + i)

                    .batch(batch)
                    .createdAt(LocalDateTime.now())
                    .build();

            records.add(record);
        }

        List<Record> savedRecords =
                recordRepository.saveAll(records);

        recordRepository.flush();

        savedRecords.stream()
                .map(Record::getRecordStage)
                .distinct()
                .forEach(stage ->
                        stageMerkleService.refreshStageRoot(batch, stage)
                );

        return savedRecords;
    }

    /*
     * Context không phải bí mật nhưng được AES-GCM xác thực bằng AAD.
     * Phải dùng cùng công thức này khi giải mã.
     */
    private String buildEncryptionContext(
            Batch batch,
            String recordKey
    ) {
        return "batch:" + batch.getId()
                + "|record:" + recordKey;
    }

    private RecordStage resolveRecordStage(RecordType recordType) {
        if (recordType == null) {
            throw new IllegalArgumentException(
                    "recordType cannot be null"
            );
        }

        return switch (recordType) {
            case PRODUCTION, HARVEST, PACKAGING ->
                    RecordStage.PRODUCER;

            case TRANSPORT, WAREHOUSE ->
                    RecordStage.DISTRIBUTOR;

            case RETAIL ->
                    RecordStage.RETAILER;
        };
    }

    private void validateRoleCanCreateRecordType(
            Role role,
            RecordType recordType
    ) {
        if (role == null) {
            throw new IllegalArgumentException(
                    "Role cannot be null"
            );
        }

        if (recordType == null) {
            throw new IllegalArgumentException(
                    "recordType cannot be null"
            );
        }

        boolean allowed = switch (role) {
            case ADMIN -> true;

            case PRODUCER ->
                    recordType == RecordType.PRODUCTION
                            || recordType == RecordType.HARVEST
                            || recordType == RecordType.PACKAGING;

            case DISTRIBUTOR ->
                    recordType == RecordType.TRANSPORT
                            || recordType == RecordType.WAREHOUSE;

            case RETAILER ->
                    recordType == RecordType.RETAIL;

            case CONSUMER -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Role " + role
                            + " is not allowed to create record type "
                            + recordType
            );
        }
    }

    private void validateRecordWritable(Batch batch) {
        if (batch == null) {
            throw new IllegalArgumentException(
                    "Batch is required"
            );
        }

        if (batch.getChainTxHash() != null
                && !batch.getChainTxHash().isBlank()) {
            throw new IllegalStateException(
                    "Cannot add records to anchored batch"
            );
        }
    }

    private String generateRecordKey(RecordType recordType) {
        String prefix = switch (recordType) {
            case PRODUCTION -> "PROD";
            case HARVEST -> "HARV";
            case PACKAGING -> "PACK";
            case TRANSPORT -> "TRAN";
            case WAREHOUSE -> "WARE";
            case RETAIL -> "RETA";
        };

        String datePart = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String randomPart = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return prefix + "-" + datePart + "-" + randomPart;
    }

    @Override
    public List<Record> getRecordsByBatchId(Long batchId) {
        return recordRepository.findByBatchId(batchId);
    }

    @Override
    public Record getByRecordKey(String recordKey) {
        Record record =
                recordRepository.findByRecordKey(recordKey);

        if (record == null) {
            throw new ResourceNotFoundException(
                    "Record not found with key: " + recordKey
            );
        }

        return record;
    }

    @Override
    public Record getByBatchIdAndRecordKey(
            Long batchId,
            String recordKey
    ) {
        return recordRepository
                .findByBatchIdAndRecordKey(batchId, recordKey)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Record not found with batchId "
                                        + batchId
                                        + " and recordKey "
                                        + recordKey
                        )
                );
    }
}