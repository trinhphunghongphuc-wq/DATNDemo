package com.service.impl;


import com.dto.user.retailer.CreateRetailUnitRequest;
import com.dto.user.retailer.RetailUnitResponse;
import com.entity.Batch;
import com.entity.RetailUnit;
import com.enums.BatchStatus;
import com.enums.RetailUnitStatus;
import com.enums.RetailUnitType;
import com.repository.BatchRepository;
import com.repository.RetailUnitRepository;
import com.service.RetailUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetailUnitServiceImpl implements RetailUnitService {

    private final BatchRepository batchRepository;
    private final RetailUnitRepository retailUnitRepository;

    @Override
    @Transactional
    public RetailUnitResponse createRetailUnit(Long batchId, Long retailerId, CreateRetailUnitRequest request) {
        Batch batch = getBatchForRetailer(batchId, retailerId);

        if (batch.getStatus() != BatchStatus.AT_RETAIL) {
            throw new RuntimeException("Batch is not at retail yet");
        }

        if (batch.getRemainingWeight() == null) {
            throw new RuntimeException("Batch remaining weight is not initialized");
        }

        if (request.getAllocatedWeight() == null || request.getAllocatedWeight() <= 0) {
            throw new RuntimeException("Allocated weight must be greater than 0");
        }

        if (batch.getRemainingWeight() < request.getAllocatedWeight()) {
            throw new RuntimeException("Not enough remaining weight in batch");
        }

        validateRetailUnitRequest(request);

        String retailCode = generateRetailCode();
        String qrContent = "http://localhost:3000/trace/retail/" + retailCode;

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

        batch.setRemainingWeight(batch.getRemainingWeight() - request.getAllocatedWeight());

        if (batch.getRemainingWeight() <= 0) {
            batch.setRemainingWeight(0.0);
            batch.setStatus(BatchStatus.SOLD_OUT);
        }

        batchRepository.save(batch);

        RetailUnit savedRetailUnit = retailUnitRepository.save(retailUnit);

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
                .build();
    }
}