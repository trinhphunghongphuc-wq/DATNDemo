package com.dto.user.distributor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TransportRouteRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double originLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double originLon,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double destinationLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double destinationLon
) {}