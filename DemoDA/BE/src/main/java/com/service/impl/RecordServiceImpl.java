package com.service.impl;

import com.dto.record.RecordRequest;
import com.entity.Batch;
import com.entity.Record;
import com.enums.RecordStage;
import com.enums.RecordType;
import com.enums.Role;
import com.exception.ResourceNotFoundException;
import com.repository.RecordRepository;
import com.service.IpfsService;
import com.service.MerkleService;
import com.service.RecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.service.StageMerkleService;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final MerkleService merkleService;
    private final IpfsService ipfsService;
    private final StageMerkleService stageMerkleService;

    public RecordServiceImpl(
            RecordRepository recordRepository,
            MerkleService merkleService,
            IpfsService ipfsService, StageMerkleService stageMerkleService
    ) {
        this.recordRepository = recordRepository;
        this.merkleService = merkleService;
        this.ipfsService = ipfsService;
        this.stageMerkleService = stageMerkleService;
    }

    @Override
    @Transactional
    public List<Record> createRecordsForBatch(
            Batch batch,
            List<RecordRequest> requests,
            Role role
    ) {
        validateRecordWritable(batch);

        List<Record> records = new ArrayList<>();

        // Vị trí của record trong toàn bộ batch.
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

            String recordKey =
                    generateRecordKey(request.getRecordType());

            String leafHash =
                    merkleService.hashRecord(request.getRawJson());

            String ipfsCid =
                    ipfsService.uploadJson(request.getRawJson());

            RecordStage recordStage =
                    resolveRecordStage(request.getRecordType());

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
                    .rawJson(request.getRawJson())
                    .ipfsCid(ipfsCid)
                    .recordStage(recordStage)
                    .stageLeafIndex(stageLeafIndex)
                    .leafHash(leafHash)
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