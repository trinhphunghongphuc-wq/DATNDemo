package com.dto.product;

import com.enums.RetailUnitType;

import java.time.LocalDate;

public record RetailUnitTraceResponse(
        String retailCode,
        String productName,
        RetailUnitType type,
        Double allocatedWeight,
        Double packageWeight,
        Integer packageQuantity,
        LocalDate expiryDate,
        String batchCode,
        Long batchId,
        String retailRecordKey,

        String producerName,
        String farmName,
        String harvestLocation,
        LocalDate harvestDate,
        RecommendedConditions recommendedConditions,
        MeasuredConditions transportConditions,
        MeasuredConditions retailerStorageConditions
) {
    public record RecommendedConditions(
            Double temperatureMin,
            Double temperatureMax,
            Double humidityMin,
            Double humidityMax
    ) {}

    public record MeasuredConditions(
            Double temperatureMin,
            Double temperatureMax,
            Double humidityMin,
            Double humidityMax,
            String startedAt,
            String endedAt,
            int sampleCount,
            Integer outOfRangeCount
    ) {}
}