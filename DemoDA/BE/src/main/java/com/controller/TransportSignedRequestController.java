package com.controller;

import com.dto.user.distributor.TransportSensorRecordRequest;
import com.entity.Batch;
import com.entity.Record;
import com.enums.RecordType;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.security.CustomUserDetails;
import com.service.DataEncryptionService;
import com.service.IpfsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/distributor/batches/{batchId}/transport-records")
@PreAuthorize("hasRole('DISTRIBUTOR')")
@ConditionalOnProperty(
        name = "app.demo-device.enabled",
        havingValue = "true"
)
public class TransportSignedRequestController {

    private final BatchRepository batchRepository;
    private final RecordRepository recordRepository;
    private final IpfsService ipfsService;
    private final DataEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{recordKey}/signed-request")
    public TransportSensorRecordRequest getSignedRequest(
            @PathVariable Long batchId,
            @PathVariable String recordKey,
            Authentication authentication
    ) throws Exception {
        Long distributorId =
                ((CustomUserDetails) authentication.getPrincipal()).getId();

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Batch not found"));

        if (!Objects.equals(batch.getDistributorId(), distributorId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Batch does not belong to this distributor"
            );
        }

        Record record = recordRepository
                .findByBatchIdAndRecordKey(batchId, recordKey)
                .orElseThrow(() ->
                        new IllegalArgumentException("Record not found"));

        if (record.getRecordType() != RecordType.TRANSPORT) {
            throw new IllegalArgumentException(
                    "Record is not a TRANSPORT point"
            );
        }

        String rawJson = record.getRawJson();

        if (Boolean.TRUE.equals(record.getEncrypted())) {
            if (record.getIpfsCid() == null
                    || record.getIpfsCid().isBlank()) {
                throw new IllegalStateException(
                        "Transport record has no IPFS CID"
                );
            }

            rawJson = encryptionService.decrypt(
                    ipfsService.getJson(record.getIpfsCid()),
                    "batch:" + batchId
                            + "|record:" + record.getRecordKey()
            );
        }

        JsonNode data = objectMapper.readTree(rawJson);

        if (data.path("batchId").asLong(-1) != batchId
                || !data.hasNonNull("vehicleId")
                || !data.hasNonNull("sequence")
                || !data.hasNonNull("gps")
                || !data.hasNonNull("temperature")
                || !data.hasNonNull("humidity")
                || !data.hasNonNull("timestamp")
                || !data.hasNonNull("deviceSignature")) {
            throw new IllegalStateException(
                    "Stored record is not a complete signed transport request"
            );
        }

        TransportSensorRecordRequest response =
                new TransportSensorRecordRequest();
        response.setVehicleId(data.get("vehicleId").asLong());
        response.setSequence(data.get("sequence").asLong());
        response.setGps(data.get("gps").asText());
        response.setTemperature(data.get("temperature").asDouble());
        response.setHumidity(data.get("humidity").asDouble());
        response.setTimestamp(data.get("timestamp").asText());
        response.setDeviceSignature(data.get("deviceSignature").asText());

        return response;
    }
}