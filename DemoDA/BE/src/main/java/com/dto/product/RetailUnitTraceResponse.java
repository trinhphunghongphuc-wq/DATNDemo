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
        String retailRecordKey
) {}