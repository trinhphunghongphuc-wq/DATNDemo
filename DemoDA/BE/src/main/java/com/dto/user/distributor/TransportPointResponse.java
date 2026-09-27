package com.dto.user.distributor;

public record TransportPointResponse(
        String recordKey,
        long vehicleId,
        long sequence,
        double latitude,
        double longitude,
        double temperature,
        double humidity,
        String timestamp
) {}