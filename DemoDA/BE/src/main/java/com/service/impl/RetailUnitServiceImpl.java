package com.service.impl;



import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.retailer.RetailStorageReadingRequest;
import com.entity.Record;
import com.dto.user.retailer.CreateRetailUnitRequest;
import com.dto.user.retailer.RetailUnitResponse;
import com.entity.Batch;
import com.entity.RetailUnit;
import com.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.RetailUnitRepository;
import com.service.RecordService;
import com.service.RetailUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.enums.RecordType;
import com.enums.Role;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RetailUnitServiceImpl implements RetailUnitService {

    private final BatchRepository batchRepository;
    private final RetailUnitRepository retailUnitRepository;

    private final RecordService recordService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RetailUnitResponse createRetailUnit(
            Long batchId,
            Long retailerId,
            CreateRetailUnitRequest request
    ) {
        Batch batch = getBatchForRetailer(batchId, retailerId);

        if (batch.getStatus() != BatchStatus.AT_RETAIL) {
            throw new RuntimeException("Batch is not at retail yet");
        }

        if (batch.getRemainingWeight() == null) {
            throw new RuntimeException("Batch remaining weight is not initialized");
        }

        if (request.getAllocatedWeight() == null
                || request.getAllocatedWeight() <= 0) {
            throw new RuntimeException(
                    "Allocated weight must be greater than 0");
        }

        if (batch.getRemainingWeight() < request.getAllocatedWeight()) {
            throw new RuntimeException(
                    "Not enough remaining weight in batch");
        }

        validateRetailUnitRequest(request);

        String retailCode = generateRetailCode();
        String qrContent = "/trace/retail/" + retailCode;

        RetailUnit retailUnit = RetailUnit.builder()
                .retailCode(retailCode)
                .productName(request.getProductName())
                .type(request.getType())
                .status(RetailUnitStatus.IN_STOCK)
                .allocatedWeight(request.getAllocatedWeight())
                .packageWeight(request.getPackageWeight())
                .packageQuantity(request.getPackageQuantity())
                .pricePerPackage(request.getPricePerPackage())
                .pricePerKg(request.getPricePerKg())
                .expiryDate(batch.getExpiryDate())
                .qrContent(qrContent)
                .retailerId(retailerId)
                .batch(batch)
                .createdAt(LocalDateTime.now())
                .build();

        RetailUnit savedRetailUnit = retailUnitRepository.save(retailUnit);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "RETAIL_UNIT_CREATED");
        event.put("batchId", batch.getId());
        event.put("retailCode", savedRetailUnit.getRetailCode());
        event.put("retailerId", retailerId);
        event.put("productName", savedRetailUnit.getProductName());
        event.put("type", savedRetailUnit.getType().name());
        event.put("allocatedWeightKg", savedRetailUnit.getAllocatedWeight());
        event.put("packageWeightKg", savedRetailUnit.getPackageWeight());
        event.put("packageQuantity", savedRetailUnit.getPackageQuantity());
        event.put("createdAt", savedRetailUnit.getCreatedAt().toString());

        RecordRequest recordRequest = new RecordRequest();
        recordRequest.setRecordType(RecordType.RETAIL);
        recordRequest.setRawJson(objectMapper.valueToTree(event).toString());
        recordRequest.setPrivateData(false);

        Record retailRecord = recordService.createRecordsForBatch(
                batch,
                List.of(recordRequest),
                Role.RETAILER
        ).get(0);

        savedRetailUnit.setRetailRecordKey(retailRecord.getRecordKey());

        // Số đo đầu tiên thuộc đúng unit vừa tạo, không thuộc chung cả batch.
        if (request.getInitialStorageReading() != null) {
            addStorageReading(
                    savedRetailUnit.getId(),
                    retailerId,
                    request.getInitialStorageReading()
            );
        }

        batch.setRemainingWeight(
                batch.getRemainingWeight() - request.getAllocatedWeight()
        );

        if (batch.getRemainingWeight() <= 0) {
            batch.setRemainingWeight(0.0);
        }

        batchRepository.save(batch);
        retailUnitRepository.save(savedRetailUnit);

        return toResponse(savedRetailUnit);
    }


    @Override
    @Transactional(readOnly = true)
    public List<RetailUnitResponse> getMyRetailUnits(Long retailerId) {
        return retailUnitRepository.findByRetailerId(retailerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetailUnitResponse> getRetailUnitsByBatch(Long batchId, Long retailerId) {
        getBatchForRetailer(batchId, retailerId);

        return retailUnitRepository.findByBatchIdAndRetailerId(batchId, retailerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RetailUnitResponse getRetailUnitDetail(Long retailUnitId, Long retailerId) {
        RetailUnit retailUnit = retailUnitRepository.findById(retailUnitId)
                .orElseThrow(() -> new RuntimeException("Retail unit not found with id: " + retailUnitId));

        if (!Objects.equals(retailUnit.getRetailerId(), retailerId)) {
            throw new RuntimeException("Retail unit does not belong to this retailer");
        }

        return toResponse(retailUnit);
    }

    @Override
    @Transactional
    public RecordItemResponse addStorageReading(
            Long retailUnitId,
            Long retailerId,
            RetailStorageReadingRequest request
    ) {
        RetailUnit unit = retailUnitRepository.findById(retailUnitId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Retail unit not found: " + retailUnitId));

        if (!Objects.equals(unit.getRetailerId(), retailerId)) {
            throw new IllegalArgumentException(
                    "Retail unit does not belong to this retailer");
        }

        Batch batch = unit.getBatch();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eventType", "RETAIL_STORAGE_READING");
        data.put("retailUnitId", unit.getId());
        data.put("retailCode", unit.getRetailCode());
        data.put("location", request.location());
        data.put("temperature", request.temperature());
        data.put("humidity", request.humidity());
        data.put("note", request.note());
        data.put("readingSource", "MANUAL_ENTRY");
        // Thời điểm ghi nhận đã có trong record.createdAt do BE tạo.

        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize storage reading", e);
        }

        RecordRequest recordRequest = new RecordRequest();
        recordRequest.setRecordType(RecordType.RETAIL);
        recordRequest.setRawJson(rawJson);
        recordRequest.setPrivateData(true);

        com.entity.Record saved = recordService.createRecordsForBatch(
                batch,
                List.of(recordRequest),
                Role.RETAILER
        ).get(0);

        return RecordItemResponse.builder()
                .recordId(saved.getId())
                .recordKey(saved.getRecordKey())
                .recordType(saved.getRecordType())
                .rawJson(saved.getRawJson())
                .leafHash(saved.getLeafHash())
                .leafIndex(saved.getLeafIndex())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private Batch getBatchForRetailer(Long batchId, Long retailerId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        if (!Objects.equals(batch.getRetailerId(), retailerId)) {
            throw new RuntimeException("Batch not assigned to this retailer");
        }

        return batch;
    }

    private void validateRetailUnitRequest(CreateRetailUnitRequest request) {
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            throw new RuntimeException("Product name is required");
        }

        if (request.getType() == null) {
            throw new RuntimeException("Retail unit type is required");
        }

        if (request.getType() == RetailUnitType.PACKAGED) {
            if (request.getPackageWeight() == null || request.getPackageWeight() <= 0) {
                throw new RuntimeException("Package weight is required for PACKAGED product");
            }

            if (request.getPackageQuantity() == null || request.getPackageQuantity() <= 0) {
                throw new RuntimeException("Package quantity is required for PACKAGED product");
            }

            if (request.getPricePerPackage() == null || request.getPricePerPackage() <= 0) {
                throw new RuntimeException("Price per package is required for PACKAGED product");
            }

            double expectedWeight = request.getPackageWeight() * request.getPackageQuantity();

            if (Math.abs(expectedWeight - request.getAllocatedWeight()) > 0.001) {
                throw new RuntimeException("Allocated weight must equal packageWeight * packageQuantity");
            }
        }

        if (request.getType() == RetailUnitType.BULK) {
            if (request.getPricePerKg() == null || request.getPricePerKg() <= 0) {
                throw new RuntimeException("Price per kg is required for BULK product");
            }
        }
    }

    private String generateRetailCode() {
        return "RETAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private RetailUnitResponse toResponse(RetailUnit retailUnit) {
        Batch batch = retailUnit.getBatch();

        return RetailUnitResponse.builder()
                .id(retailUnit.getId())
                .retailCode(retailUnit.getRetailCode())
                .productName(retailUnit.getProductName())
                .type(retailUnit.getType())
                .status(retailUnit.getStatus())
                .allocatedWeight(retailUnit.getAllocatedWeight())
                .packageWeight(retailUnit.getPackageWeight())
                .packageQuantity(retailUnit.getPackageQuantity())
                .pricePerPackage(retailUnit.getPricePerPackage())
                .pricePerKg(retailUnit.getPricePerKg())
                .expiryDate(retailUnit.getExpiryDate())
                .qrContent(retailUnit.getQrContent())
                .retailerId(retailUnit.getRetailerId())
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getName())
                .batchRemainingWeight(batch.getRemainingWeight())
                .createdAt(retailUnit.getCreatedAt())
                .retailRecordKey(retailUnit.getRetailRecordKey())
                .build();
    }
}