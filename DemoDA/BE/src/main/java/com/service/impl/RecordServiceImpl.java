package com.service.impl;

import com.dto.record.RecordRequest;
import com.entity.Batch;
import com.entity.Record;
import com.enums.RecordType;
import com.enums.Role;
import com.exception.ResourceNotFoundException;
import com.repository.RecordRepository;
import com.service.MerkleService;
import com.service.RecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final MerkleService merkleService;

    public RecordServiceImpl(RecordRepository recordRepository, MerkleService merkleService) {
        this.recordRepository = recordRepository;
        this.merkleService = merkleService;
    }


    @Override
    public List<Record> createRecordsForBatch(Batch batch, List<RecordRequest> requests, Role role) {
        validateRecordWritable(batch);

        List<Record> records = new ArrayList<>();
        int startIndex = recordRepository.findByBatchId(batch.getId()).size();

        for (int i = 0; i < requests.size(); i++) {
            RecordRequest request = requests.get(i);

            validateRoleCanCreateRecordType(role, request.getRecordType());

            String recordKey = generateRecordKey(request.getRecordType());
            String leafHash = merkleService.hashRecord(request.getRawJson());

            Record record = Record.builder()
                    .recordKey(recordKey)
                    .recordType(request.getRecordType())
                    .rawJson(request.getRawJson())
                    .leafHash(leafHash)
                    .leafIndex(startIndex + i)
                    .batch(batch)
                    .createdAt(LocalDateTime.now())
                    .build();

            records.add(record);
        }

        return recordRepository.saveAll(records);
    }

    private void validateRoleCanCreateRecordType(Role role, RecordType recordType) {
        boolean allowed = switch (role) {
            case ADMIN -> true;
            case PRODUCER -> recordType == RecordType.PRODUCTION
                    || recordType == RecordType.HARVEST
                    || recordType == RecordType.PACKAGING;

            case DISTRIBUTOR -> recordType == RecordType.TRANSPORT
                    || recordType == RecordType.WAREHOUSE;

            case RETAILER -> recordType == RecordType.RETAIL;
            case CONSUMER -> false;
        };

        if (!allowed) {
            throw new RuntimeException("Role " + role + " is not allowed to create record type " + recordType);
        }
    }

    private void validateRecordWritable(Batch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch is required");
        }

        if (batch.getChainTxHash() != null && !batch.getChainTxHash().isBlank()) {
            throw new IllegalStateException("Cannot add records to anchored batch");
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

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return prefix + "-" + datePart + "-" + randomPart;
    }

    @Override
    public List<Record> getRecordsByBatchId(Long batchId) {
        return recordRepository.findByBatchId(batchId);
    }

    @Override
    public Record getByRecordKey(String recordKey) {
        Record record = recordRepository.findByRecordKey(recordKey);
        if (record == null) {
            throw new ResourceNotFoundException("Record not found with key: " + recordKey);
        }
        return record;
    }

    @Override
    public Record getByBatchIdAndRecordKey(Long batchId, String recordKey) {
        return recordRepository.findByBatchIdAndRecordKey(batchId, recordKey)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Record not found with batchId " + batchId + " and recordKey " + recordKey
                        )
                );
    }

    //Update này test thôi thực tế ko cho sửa và delete record
//    @Override
//    public Record updateRecord(String recordKey, RecordRequest request, Role role) {
//        Record record = recordRepository.findByRecordKey(recordKey);
//        if (record == null) {
//            throw new ResourceNotFoundException("Record not found with key: " + recordKey);
//        }
//
//        Batch batch = record.getBatch();
//        validateRecordWritable(batch);
//
//        validateRoleCanCreateRecordType(role, request.getRecordType());
//
//        record.setRawJson(request.getRawJson());
//        record.setRecordType(request.getRecordType());
//        record.setLeafHash(merkleService.hashRecord(request.getRawJson()));
//
//        return recordRepository.save(record);
//    }
//
//    @Override
//    public void deleteRecord(String recordKey) {
//        Record record = recordRepository.findByRecordKey(recordKey);
//        if (record == null) {
//            throw new ResourceNotFoundException("Record not found with key: " + recordKey);
//        }
//
//        Batch batch = record.getBatch();
//        validateRecordWritable(batch);
//
//        recordRepository.delete(record);
//    }

}