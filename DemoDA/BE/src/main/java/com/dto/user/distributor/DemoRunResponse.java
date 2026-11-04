package com.dto.user.distributor;

public record DemoRunResponse(
        Long batchId,
        Long vehicleId,
        String status,
        int sentPoints,
        int totalPoints,
        String error
) {}