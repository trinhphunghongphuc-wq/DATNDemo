package com.service.impl;

import com.dto.product.ProductDetailResponse;
import com.dto.product.ProductListResponse;
import com.dto.product.TraceabilityResponse;
import com.dto.product.TraceabilityStepResponse;
import com.dto.record.RecordItemResponse;
import com.entity.Batch;
import com.entity.Record;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.service.JsonExtractService;
import com.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final BatchRepository batchRepository;
    private final RecordRepository recordRepository;
    private final JsonExtractService jsonExtractService;

    @Override
    public List<ProductListResponse> getAllProducts() {
        List<Batch> batches = batchRepository.findAll();

        return batches.stream()
                .map(batch -> {
                    Record firstRecord = getFirstRecord(batch.getId());
                    Map<String, String> rawData = jsonExtractService.extractBasicFields(firstRecord);

                    return new ProductListResponse(
                            batch.getId(),
                            rawData.getOrDefault("product", batch.getName()),
                            rawData.getOrDefault("origin", "UNKNOWN"),
                            rawData.getOrDefault("weight", "N/A"),
                            resolveStatus(batch)
                    );
                })
                .toList();
    }

    @Override
    public ProductDetailResponse getProductDetail(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        List<Record> records = recordRepository.findByBatchId(batchId);
        Record firstRecord = records.isEmpty() ? null : records.get(0);
        Map<String, String> rawData = jsonExtractService.extractBasicFields(firstRecord);

        List<RecordItemResponse> recordItems = records.stream()
                .map(record -> new RecordItemResponse(
                        record.getId(),
                        record.getRecordKey(),
                        record.getRawJson(),
                        record.getLeafHash(),
                        record.getLeafIndex(),
                        record.getCreatedAt(),
                        record.getRecordType()
                ))
                .toList();

        return new ProductDetailResponse(
                batch.getId(),
                batch.getName(),
                rawData.getOrDefault("product", batch.getName()),
                rawData.getOrDefault("origin", "UNKNOWN"),
                rawData.getOrDefault("weight", "N/A"),
                batch.getMerkleRoot(),
                batch.getChainTxHash(),
                batch.getCreatedAt(),
                resolveStatus(batch),
                recordItems
        );
    }

    @Override
    public TraceabilityResponse getTraceability(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        return buildTraceabilityResponse(batch);
    }

    @Override
    public TraceabilityResponse getTraceabilityByBatchCode(String batchCode) {
        Batch batch = batchRepository.findByBatchCode(batchCode)
                .orElseThrow(() ->
                        new RuntimeException("Batch not found with code: " + batchCode));

        return buildTraceabilityResponse(batch);
    }

    private TraceabilityResponse buildTraceabilityResponse(Batch batch) {
        List<Record> records = recordRepository.findByBatchId(batch.getId());

        List<TraceabilityStepResponse> steps = records.stream()
                .map(record -> new TraceabilityStepResponse(
                        record.getRecordKey(),
                        record.getRawJson(),
                        record.getLeafHash(),
                        record.getLeafIndex(),
                        record.getCreatedAt()
                ))
                .toList();

        return new TraceabilityResponse(
                batch.getId(),
                batch.getName(),
                batch.getMerkleRoot(),
                batch.getChainTxHash(),
                resolveStatus(batch),
                steps
        );
    }

    @Override
    public List<ProductListResponse> searchAndFilterProducts(
            String keyword,
            String origin,
            String status
    ) {
        List<ProductListResponse> products = getAllProducts();

        return products.stream()
                .filter(product -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(product.getProductName(), keyword)
                        || containsIgnoreCase(product.getOrigin(), keyword))
                .filter(product -> origin == null || origin.isBlank()
                        || product.getOrigin() != null
                        && product.getOrigin().equalsIgnoreCase(origin))
                .filter(product -> status == null || status.isBlank()
                        || product.getStatus() != null
                        && product.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private Record getFirstRecord(Long batchId) {
        List<Record> records = recordRepository.findByBatchId(batchId);
        return records.isEmpty() ? null : records.get(0);
    }

    private String resolveStatus(Batch batch) {
        if (batch.getChainTxHash() != null && !batch.getChainTxHash().isBlank()) {
            return "ROOT_ANCHORED";
        }
        if (batch.getMerkleRoot() != null && !batch.getMerkleRoot().isBlank()) {
            return "OFFCHAIN_READY";
        }
        return "DRAFT";
    }
}