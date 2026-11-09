package com.dto.user.retailer;

import com.enums.RetailUnitType;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.Valid;

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

    @Valid
    private RetailStorageReadingRequest initialStorageReading;

    @Size(max = 500, message = "Cách bảo quản không được quá 500 ký tự")
    private String storageInstructions;
}