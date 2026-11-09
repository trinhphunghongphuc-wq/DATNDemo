package com.dto.user.distributor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DemoRunRequest(
        @NotNull Long vehicleId,
        @NotNull @Min(2) @Max(100) Integer pointCount,
        @NotNull Double temperature,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double humidity
) {}