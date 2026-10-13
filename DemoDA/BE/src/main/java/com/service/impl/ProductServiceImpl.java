package com.service.impl;

import com.dto.product.*;
import com.dto.record.RecordItemResponse;
import com.entity.Batch;
import com.entity.Record;
import com.entity.RetailUnit;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.repository.RetailUnitRepository;
import com.service.JsonExtractService;
import com.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.entity.ProductCategory;
import com.enums.RecordType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.DataEncryptionService;
import com.service.IpfsService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final BatchRepository batchRepository;
    private final RecordRepository recordRepository;
    private final JsonExtractService jsonExtractService;
    private final RetailUnitRepository retailUnitRepository;

    private final ObjectMapper objectMapper;
    private final IpfsService ipfsService;
    private final DataEncryptionService dataEncryptionService;

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

    @Override
    @Transactional(readOnly = true)
    public RetailUnitTraceResponse getRetailUnitTrace(String retailCode) {
        RetailUnit unit = retailUnitRepository.findByRetailCode(retailCode)
                .orElseThrow(() ->
                        new IllegalArgumentException("Retail unit not found"));

        Batch batch = unit.getBatch();
        List<Record> records = recordRepository.findByBatchId(batch.getId());

        JsonNode harvest = records.stream()
                .filter(record -> record.getRecordType() == RecordType.HARVEST)
                .findFirst()
                .map(record -> readRecordData(batch, record))
                .orElse(null);

        ProductCategory category = batch.getProductCategory();

        RetailUnitTraceResponse.RecommendedConditions recommended =
                category == null ? null :
                        new RetailUnitTraceResponse.RecommendedConditions(
                                category.getTemperatureMin(),
                                category.getTemperatureMax(),
                                category.getHumidityMin(),
                                category.getHumidityMax()
                        );

        String producerName = batch.getCreatedBy() == null
                ? null
                : batch.getCreatedBy().getUsername();

        return new RetailUnitTraceResponse(
                unit.getRetailCode(),
                unit.getProductName(),
                unit.getType(),
                unit.getAllocatedWeight(),
                unit.getPackageWeight(),
                unit.getPackageQuantity(),
                unit.getExpiryDate() != null
                        ? unit.getExpiryDate()
                        : batch.getExpiryDate(),
                batch.getBatchCode(),
                batch.getId(),
                unit.getRetailRecordKey(),

                producerName,
                text(harvest, "farmName", "farm"),
                text(harvest, "fieldLocation", "harvestLocation"),
                parseDate(text(harvest, "harvestDate")),
                recommended,
                summarizeTransport(batch, records, category),
                null // Chưa có record cảm biến bảo quản tại Retailer.
        );
    }

    private RetailUnitTraceResponse.MeasuredConditions summarizeTransport(
            Batch batch,
            List<Record> records,
            ProductCategory category
    ) {
        Double minTemperature = null;
        Double maxTemperature = null;
        Double minHumidity = null;
        Double maxHumidity = null;
        Instant first = null;
        Instant last = null;
        int sampleCount = 0;
        int outOfRangeCount = 0;

        boolean hasRecommendedRange = category != null
                && (category.getTemperatureMin() != null
                || category.getTemperatureMax() != null
                || category.getHumidityMin() != null
                || category.getHumidityMax() != null);

        for (Record record : records) {
            if (record.getRecordType() != RecordType.TRANSPORT) {
                continue;
            }

            JsonNode data = readRecordData(batch, record);
            JsonNode temperatureNode = data.path("temperature");
            JsonNode humidityNode = data.path("humidity");

            // Không biến giá trị thiếu thành 0°C hoặc 0% RH.
            if (!temperatureNode.isNumber() || !humidityNode.isNumber()) {
                continue;
            }

            double temperature = temperatureNode.asDouble();
            double humidity = humidityNode.asDouble();

            if (!Double.isFinite(temperature)
                    || !Double.isFinite(humidity)
                    || humidity < 0
                    || humidity > 100) {
                continue;
            }

            minTemperature = minTemperature == null
                    ? temperature : Math.min(minTemperature, temperature);
            maxTemperature = maxTemperature == null
                    ? temperature : Math.max(maxTemperature, temperature);
            minHumidity = minHumidity == null
                    ? humidity : Math.min(minHumidity, humidity);
            maxHumidity = maxHumidity == null
                    ? humidity : Math.max(maxHumidity, humidity);

            sampleCount++;

            if (hasRecommendedRange
                    && isOutOfRange(temperature, humidity, category)) {
                outOfRangeCount++;
            }

            String timestamp = text(data, "timestamp");
            if (timestamp != null) {
                try {
                    Instant measuredAt = OffsetDateTime.parse(timestamp).toInstant();

                    if (first == null || measuredAt.isBefore(first)) {
                        first = measuredAt;
                    }
                    if (last == null || measuredAt.isAfter(last)) {
                        last = measuredAt;
                    }
                } catch (DateTimeParseException ignored) {
                    // Giữ mẫu đo; chỉ bỏ qua timestamp không đúng ISO offset.
                }
            }
        }

        if (sampleCount == 0) {
            return null;
        }

        return new RetailUnitTraceResponse.MeasuredConditions(
                minTemperature,
                maxTemperature,
                minHumidity,
                maxHumidity,
                first == null ? null : first.toString(),
                last == null ? null : last.toString(),
                sampleCount,
                hasRecommendedRange ? outOfRangeCount : null
        );
    }

    private boolean isOutOfRange(
            double temperature,
            double humidity,
            ProductCategory category
    ) {
        return category.getTemperatureMin() != null
                && temperature < category.getTemperatureMin()
                || category.getTemperatureMax() != null
                && temperature > category.getTemperatureMax()
                || category.getHumidityMin() != null
                && humidity < category.getHumidityMin()
                || category.getHumidityMax() != null
                && humidity > category.getHumidityMax();
    }

    private JsonNode readRecordData(Batch batch, Record record) {
        try {
            String rawJson = record.getRawJson();

            if (Boolean.TRUE.equals(record.getEncrypted())) {
                if (record.getIpfsCid() == null) {
                    throw new IllegalStateException(
                            "Encrypted record has no IPFS CID"
                    );
                }

                String encryptedJson = ipfsService.getJson(record.getIpfsCid());
                String context = "batch:" + batch.getId()
                        + "|record:" + record.getRecordKey();

                rawJson = dataEncryptionService.decrypt(
                        encryptedJson,
                        context
                );
            }

            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot read record " + record.getRecordKey(),
                    ex
            );
        }
    }

    private String text(JsonNode data, String... keys) {
        if (data == null) {
            return null;
        }

        for (String key : keys) {
            JsonNode value = data.path(key);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }

        return null;
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
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