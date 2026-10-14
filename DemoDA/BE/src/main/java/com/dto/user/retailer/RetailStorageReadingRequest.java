package com.dto.user.retailer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RetailStorageReadingRequest(
        @NotBlank String location,
        @NotNull Double temperature,
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double humidity,
        String note
) {}