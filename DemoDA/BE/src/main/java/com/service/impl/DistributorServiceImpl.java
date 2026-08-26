package com.service.impl;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.distributor.TransportSensorRecordRequest;
import com.dto.verify.VerifyAllResponse;
import com.entity.Batch;
import com.entity.Record;
import com.entity.Vehicle;
import com.enums.AnchorStatus;
import com.enums.BatchStatus;
import com.enums.RecordType;
import com.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.VehicleRepository;
import com.service.DistributorService;
import com.service.RecordService;
import com.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DistributorServiceImpl implements DistributorService {

    private final BatchRepository batchRepository;
    private final RecordService recordService;
    private final VerifyService verifyService;
    private final ObjectMapper objectMapper;
    private final VehicleRepository vehicleRepository;

    @Override
    public List<AdminBatchListResponse> getAssignedBatches(Long distributorId) {
        return batchRepository.findByDistributorId(distributorId)
                .stream()
                .map(this::toBatchListResponse)
                .toList();
    }

    @Override
    public AdminBatchListResponse receiveBatch(Long batchId, Long distributorId) {
        Batch batch = getBatchForDistributor(batchId, distributorId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot receive anchored batch");
        }

        if (batch.getStatus() != BatchStatus.ASSIGNED_TO_DISTRIBUTOR) {
            throw new RuntimeException("Batch is not assigned to distributor");
        }


        if (batch.getStatus() == BatchStatus.RECEIVED_BY_DISTRIBUTOR) {
            throw new RuntimeException("Batch already received");
        }

        batch.setStatus(BatchStatus.RECEIVED_BY_DISTRIBUTOR);
        Batch savedBatch = batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }

    @Override
    public RecordItemResponse addTransportRecord(
            Long batchId,
            TransportSensorRecordRequest request,
            Long distributorId
    ) {
        Batch batch = getBatchForDistributor(batchId, distributorId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot add record to anchored batch");
        }

        if (batch.getStatus() != BatchStatus.RECEIVED_BY_DISTRIBUTOR
                && batch.getStatus() != BatchStatus.IN_DISTRIBUTION) {
            throw new RuntimeException("Distributor must receive batch before adding transport record");
        }

        Vehicle vehicle = vehicleRepository
                .findByIdAndDistributorIdAndActiveTrue(request.getVehicleId(), distributorId)
                .orElseThrow(() -> new RuntimeException(
                        "Vehicle not found or does not belong to this distributor"
                ));

        String deviceSignature = generateDeviceSignature(vehicle, request);

        try {
            Map<String, Object> transportPayload = new LinkedHashMap<>();
            transportPayload.put("vehicleId", vehicle.getId());
            transportPayload.put("vehiclePlate", vehicle.getVehiclePlate());
            transportPayload.put("deviceId", vehicle.getDeviceId());
            transportPayload.put("sensorFirmware", vehicle.getSensorFirmware());
            transportPayload.put("gps", request.getGps());
            transportPayload.put("temperature", request.getTemperature());
            transportPayload.put("humidity", request.getHumidity());
            transportPayload.put("timestamp", request.getTimestamp());
            transportPayload.put("deviceSignature", deviceSignature);

            RecordRequest recordRequest = new RecordRequest();
            recordRequest.setRecordType(RecordType.TRANSPORT);
            recordRequest.setRawJson(objectMapper.writeValueAsString(transportPayload));

            batch.setStatus(BatchStatus.IN_DISTRIBUTION);
            batchRepository.save(batch);

            Record savedRecord = recordService.createRecordsForBatch(
                    batch,
                    List.of(recordRequest),
                    Role.DISTRIBUTOR
            ).get(0);

            return RecordItemResponse.builder()
                    .recordId(savedRecord.getId())
                    .recordKey(savedRecord.getRecordKey())
                    .recordType(savedRecord.getRecordType())
                    .rawJson(savedRecord.getRawJson())
                    .leafHash(savedRecord.getLeafHash())
                    .leafIndex(savedRecord.getLeafIndex())
                    .createdAt(savedRecord.getCreatedAt())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Cannot create transport sensor record");
        }
    }

    private String generateDeviceSignature(
            Vehicle vehicle,
            TransportSensorRecordRequest request
    ) {

        String payload =
                vehicle.getId() + "|" +
                        vehicle.getVehiclePlate() + "|" +
                        vehicle.getDeviceId() + "|" +
                        vehicle.getSensorFirmware() + "|" +
                        request.getGps() + "|" +
                        request.getTemperature() + "|" +
                        request.getHumidity() + "|" +
                        request.getTimestamp();

        return Hash.sha3(payload);
    }

    @Override
    public AdminBatchListResponse DeliveryToDetailer(Long batchId, Long distributorId) {
        Batch batch = getBatchForDistributor(batchId, distributorId);

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot confirm delivery for anchored batch");
        }

        if (batch.getStatus() != BatchStatus.IN_DISTRIBUTION) {
            throw new RuntimeException("Batch is not in distribution");
        }

        batch.setStatus(BatchStatus.DELIVERED_TO_RETAILER);
        Batch savedBatch = batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }

    @Override
    public BatchDetailResponse getBatchDetail(Long batchId, Long distributorId) {
        Batch batch = getBatchForDistributor(batchId, distributorId);
        return toBatchDetailResponse(batch);
    }

    @Override
    public VerifyAllResponse verifyAllRecords(Long batchId, Long distributorId) {
        Batch batch = getBatchForDistributor(batchId, distributorId);
        return verifyService.verifyAllRecords(batch.getId());
    }

//    private Batch getBatchForDistributor(Long batchId, Long distributorId) {
//        return batchRepository.findByIdAndDistributorId(batchId, distributorId)
//                .orElseThrow(() -> new RuntimeException("Batch not found or not assigned to this distributor"));
//    }

    private Batch getBatchForDistributor(Long batchId, Long distributorId) {
        System.out.println("===== GET BATCH FOR DISTRIBUTOR DEBUG =====");
        System.out.println("input batchId = " + batchId);
        System.out.println("input distributorId = " + distributorId);

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        System.out.println("DB batch id = " + batch.getId());
        System.out.println("DB batch name = " + batch.getName());
        System.out.println("DB distributorId = " + batch.getDistributorId());
        System.out.println("DB retailerId = " + batch.getRetailerId());
        System.out.println("DB status = " + batch.getStatus());
        System.out.println("DB anchorStatus = " + batch.getAnchorStatus());

        if (!Objects.equals(batch.getDistributorId(), distributorId)) {
            throw new RuntimeException(
                    "Batch not assigned to this distributor. DB distributorId="
                            + batch.getDistributorId()
                            + ", current distributorId="
                            + distributorId
            );
        }

        return batch;
    }



    private AdminBatchListResponse toBatchListResponse(Batch batch) {
        return AdminBatchListResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .createdAt(batch.getCreatedAt())
                .status(String.valueOf(batch.getStatus()))
                .anchorStatus(String.valueOf(batch.getAnchorStatus()))
                .recordCount(batch.getRecords() == null ? 0 : batch.getRecords().size())
                .build();
    }

    private BatchDetailResponse toBatchDetailResponse(Batch batch) {
        return BatchDetailResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .batchCode(batch.getBatchCode())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .status(batch.getStatus())
                .anchorStatus(batch.getAnchorStatus())
                .createdAt(batch.getCreatedAt())
                .recordCount(batch.getRecords() == null ? 0 : batch.getRecords().size())
                .records(
                        batch.getRecords()
                                .stream()
                                .map(record -> RecordItemResponse.builder()
                                        .recordId(record.getId())
                                        .recordKey(record.getRecordKey())
                                        .recordType(record.getRecordType())
                                        .rawJson(record.getRawJson())
                                        .leafHash(record.getLeafHash())
                                        .leafIndex(record.getLeafIndex())
                                        .createdAt(record.getCreatedAt())
                                        .build())
                                .toList()
                )
                .build();
    }
}