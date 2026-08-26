package com.dto.user.retailer;

import com.enums.RetailUnitStatus;
import com.enums.RetailUnitType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RetailUnitResponse {

    private Long id;

    private String retailCode;

    private String productName;

    private RetailUnitType type;

    private RetailUnitStatus status;

    private Double allocatedWeight;

    // PACKAGED
    private Double packageWeight;

    private Integer packageQuantity;

    private Double pricePerPackage;

    // BULK
    private Double pricePerKg;

    private LocalDate expiryDate;

    private String qrContent;

    private Long retailerId;

    // Batch info
    private Long batchId;

    private String batchCode;

    private String batchName;

    private Double batchRemainingWeight;

    private LocalDateTime createdAt;
}

