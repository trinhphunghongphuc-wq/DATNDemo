package com.dto.user.retailer;

import com.enums.RetailUnitType;
import lombok.Data;

@Data
public class CreateRetailUnitRequest {

    private String productName;

    private RetailUnitType type;

    private Double allocatedWeight;

    // PACKAGED
    private Double packageWeight;
    private Integer packageQuantity;
    private Double pricePerPackage;

    // BULK
    private Double pricePerKg;
}