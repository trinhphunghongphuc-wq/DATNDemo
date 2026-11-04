package com.dto.user.distributor;

public record TransportSignatureEvidenceResponse(
        String recordKey,
        Long batchId,
        Long vehicleId,
        String deviceId,
        Long sequence,
        String timestamp,
        String publicKeyFingerprint,
        Boolean signatureValid,
        String message
) {}