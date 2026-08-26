package com.service.impl;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.producer.RejectDeliveryRequest;
import com.dto.verify.VerifyAllResponse;
import com.entity.Batch;
import com.entity.Record;
import com.enums.AnchorStatus;
import com.enums.BatchStatus;
import com.enums.RecordType;
import com.enums.Role;
import com.repository.BatchRepository;
import com.service.RecordService;
import com.service.RetailerService;
import com.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RetailerServiceImpl implements RetailerService {

    private final BatchRepository batchRepository;
    private final RecordService recordService;
    private final VerifyService verifyService;

    @Override
    public List<AdminBatchListResponse> getReceivedBatches(Long retailerId) {
        return batchRepository.findByRetailerId(retailerId)
                .stream()
                .map(this::toBatchListResponse)
                .toList();
    }

    @Override
    public AdminBatchListResponse receiveBatch(Long batchId, Long retailerId) {
        Batch batch = getBatchForRetailer(batchId, retailerId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot receive anchored batch");
        }

        if (batch.getStatus() != BatchStatus.RECEIVED_BY_DISTRIBUTOR
                && batch.getStatus() != BatchStatus.IN_DISTRIBUTION) {
            throw new RuntimeException("Batch is not ready for retailer");
        }

        batch.setStatus(BatchStatus.AT_RETAIL);
        Batch savedBatch = batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }

    @Override
    public RecordItemResponse addRetailRecord(Long batchId, RecordRequest request, Long retailerId) {
        Batch batch = getBatchForRetailer(batchId, retailerId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot add record to anchored batch");
        }

        if (batch.getStatus() != BatchStatus.AT_RETAIL) {
            throw new RuntimeException("Retailer must receive batch before adding retail record");
        }

        if (request.getRecordType() != RecordType.RETAIL) {
            throw new RuntimeException("Retailer can only add RETAIL records");
        }

        Record savedRecord = recordService.createRecordsForBatch(
                batch,
                List.of(request),
                Role.RETAILER
        ).get(0);

        return RecordItemResponse.builder()
                .recordId(savedRecord.getId())
                .recordKey(savedRecord.getRecordKey())
                .recordType(savedRecord.getRecordType())
                .rawJson(savedRecord.getRawJson())
                .leafHash(savedRecord.getLeafHash())
                .leafIndex(savedRecord.getLeafIndex())
                .createdAt(savedRecord.getCreatedAt())
                .build();
    }

    @Override
    public Map<String, Object> getExpiryStatus(Long batchId, Long retailerId) {
        Batch batch = getBatchForRetailer(batchId, retailerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batch.getId());
        response.put("batchName", batch.getName());
        response.put("batchCode", batch.getBatchCode());
        response.put("expiryDate", batch.getExpiryDate());

        if (batch.getExpiryDate() == null) {
            response.put("status", "NO_EXPIRY_DATE");
            response.put("message", "Batch has no expiry date");
            return response;
        }

        LocalDate today = LocalDate.now();
        long daysLeft = ChronoUnit.DAYS.between(today, batch.getExpiryDate());

        response.put("daysLeft", daysLeft);

        if (daysLeft < 0) {
            response.put("status", "EXPIRED");
            response.put("message", "Batch is expired");
        } else if (daysLeft <= 7) {
            response.put("status", "NEAR_EXPIRY");
            response.put("message", "Batch is near expiry");
        } else {
            response.put("status", "VALID");
            response.put("message", "Batch is still valid");
        }

        return response;
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long batchId, Long retailerId) {
        Batch batch = getBatchForRetailer(batchId, retailerId);
        return toBatchDetailResponse(batch);
    }

    @Override
    public VerifyAllResponse verifyAllRecords(Long batchId, Long retailerId) {
        Batch batch = getBatchForRetailer(batchId, retailerId);
        return verifyService.verifyAllRecords(batch.getId());
    }

    @Override
    public Map<String, Object> getVerificationQr(Long batchId, Long retailerId) {
        Batch batch = getBatchForRetailer(batchId, retailerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batch.getId());
        response.put("batchCode", batch.getBatchCode());
        response.put("verifyUrl", "/verify?batchCode=" + batch.getBatchCode());

        return response;
    }

    private Batch getBatchForRetailer(Long batchId, Long retailerId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        if (!Objects.equals(batch.getRetailerId(), retailerId)) {
            throw new RuntimeException("Batch not assigned to this retailer");
        }

        return batch;
    }


    @Override
    public AdminBatchListResponse confirmDelivery(Long batchId, Long retailerId) {

        Batch batch = getBatchForRetailer(batchId, retailerId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot confirm anchored batch");
        }

        if (batch.getStatus() != BatchStatus.DELIVERED_TO_RETAILER) {
            throw new RuntimeException(
                    "Batch is not delivered to retailer yet"
            );
        }

        // Retailer xác nhận hàng OK
        batch.setStatus(BatchStatus.AT_RETAIL);

        Batch savedBatch = batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }

    @Override
    public AdminBatchListResponse rejectDelivery(
            Long batchId,
            Long retailerId,
            RejectDeliveryRequest request
    ) {

        Batch batch = getBatchForRetailer(batchId, retailerId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot reject anchored batch");
        }

        if (batch.getStatus() != BatchStatus.DELIVERED_TO_RETAILER) {
            throw new RuntimeException("Batch is not delivered to retailer yet");
        }

        batch.setStatus(BatchStatus.DELIVERY_REJECTED);

        Batch savedBatch = batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }



    private AdminBatchListResponse toBatchListResponse(Batch batch) {
        return AdminBatchListResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .createdAt(batch.getCreatedAt())
                .status(String.valueOf(batch.getStatus()))
                .anchorStatus(String.valueOf(batch.getAnchorStatus()))
                .recordCount(batch.getRecords() == null ? 0 : batch.getRecords().size())
                .build();
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
}