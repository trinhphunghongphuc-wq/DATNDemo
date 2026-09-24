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
import com.service.DeviceSignatureService;
import com.service.DistributorService;
import com.service.RecordService;
import com.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DeviceSignatureService deviceSignatureService;

    @Override
    public List<AdminBatchListResponse> getAssignedBatches(
            Long distributorId
    ) {
        return batchRepository.findByDistributorId(distributorId)
                .stream()
                .map(this::toBatchListResponse)
                .toList();
    }

    @Override
    public AdminBatchListResponse receiveBatch(
            Long batchId,
            Long distributorId
    ) {
        Batch batch = getBatchForDistributor(
                batchId,
                distributorId
        );

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new IllegalStateException(
                    "Cannot receive anchored batch"
            );
        }

        if (batch.getStatus() == BatchStatus.RECEIVED_BY_DISTRIBUTOR) {
            throw new IllegalStateException(
                    "Batch already received"
            );
        }

        if (batch.getStatus()
                != BatchStatus.ASSIGNED_TO_DISTRIBUTOR) {
            throw new IllegalStateException(
                    "Batch is not assigned to distributor"
            );
        }

        batch.setStatus(
                BatchStatus.RECEIVED_BY_DISTRIBUTOR
        );

        Batch savedBatch =
                batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }

    @Override
    @Transactional
    public RecordItemResponse addTransportRecord(
            Long batchId,
            TransportSensorRecordRequest request,
            Long distributorId
    ) {
        Batch batch = getBatchForDistributor(
                batchId,
                distributorId
        );

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new IllegalStateException(
                    "Cannot add record to anchored batch"
            );
        }

        if (batch.getStatus()
                != BatchStatus.RECEIVED_BY_DISTRIBUTOR
                && batch.getStatus()
                != BatchStatus.IN_DISTRIBUTION) {

            throw new IllegalStateException(
                    "Distributor must receive batch before adding transport record"
            );
        }

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Khóa hàng Vehicle trong database khi xử lý sequence.
         *
         * Nếu hai request của cùng thiết bị đến đồng thời,
         * chỉ một request được quyền kiểm tra và cập nhật sequence
         * tại một thời điểm.
         */
        Vehicle vehicle = vehicleRepository
                .findActiveVehicleForUpdate(
                        request.getVehicleId(),
                        distributorId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vehicle not found or does not belong to this distributor"
                        )
                );

        if (vehicle.getDevicePublicKey() == null
                || vehicle.getDevicePublicKey().isBlank()) {

            throw new IllegalStateException(
                    "Vehicle device public key is not registered"
            );
        }

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Thiết bị phải gửi sequence tăng tuần tự.
         *
         * Chữ ký hợp lệ nhưng có sequence cũ vẫn bị từ chối,
         * nhờ đó ngăn việc phát lại một bản tin IoT đã ký trước đó.
         */
        long lastSequence =
                vehicle.getLastSequence() == null
                        ? 0L
                        : vehicle.getLastSequence();

        long expectedSequence = lastSequence + 1L;

        if (!request.getSequence().equals(expectedSequence)) {
            throw new IllegalArgumentException(
                    "Invalid device sequence. Expected "
                            + expectedSequence
                            + " but received "
                            + request.getSequence()
            );
        }

        try {
            /*
             * Payload này chưa chứa deviceSignature.
             * Thiết bị phải ký chính xác chuỗi JSON được tạo ra
             * theo đúng thứ tự field bên dưới.
             */
            Map<String, Object> transportPayload =
                    buildUnsignedTransportPayload(
                            batch,
                            vehicle,
                            request
                    );

            String signaturePayload =
                    objectMapper.writeValueAsString(
                            transportPayload
                    );

            /*
             * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
             * Backend không tự tạo chữ ký thay thiết bị.
             *
             * Thiết bị ký bằng private key Ed25519.
             * Backend chỉ xác minh bằng public key đã đăng ký
             * trong Vehicle.
             */
            boolean signatureValid =
                    deviceSignatureService.verifySignature(
                            signaturePayload,
                            request.getDeviceSignature(),
                            vehicle.getDevicePublicKey()
                    );

            if (!signatureValid) {
                throw new IllegalArgumentException(
                        "Invalid device signature"
                );
            }

            /*
             * Chỉ cập nhật sequence sau khi:
             * 1. Sequence đúng thứ tự.
             * 2. Chữ ký thiết bị hợp lệ.
             *
             * Nếu tạo record hoặc upload IPFS thất bại,
             * @Transactional sẽ rollback sequence này.
             */
            vehicle.setLastSequence(
                    request.getSequence()
            );

            vehicleRepository.save(vehicle);

            // Lưu chữ ký cùng dữ liệu cảm biến.
            transportPayload.put(
                    "deviceSignature",
                    request.getDeviceSignature()
            );

            RecordRequest recordRequest =
                    new RecordRequest();

            recordRequest.setRecordType(
                    RecordType.TRANSPORT
            );

            recordRequest.setRawJson(
                    objectMapper.writeValueAsString(
                            transportPayload
                    )
            );

            /*
             * GPS và dữ liệu cảm biến được xem là dữ liệu riêng tư.
             * RecordService sẽ mã hóa bằng AES-256-GCM
             * trước khi upload lên IPFS.
             */
            recordRequest.setPrivateData(true);

            batch.setStatus(
                    BatchStatus.IN_DISTRIBUTION
            );

            batchRepository.save(batch);

            Record savedRecord =
                    recordService.createRecordsForBatch(
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

        } catch (RuntimeException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Cannot create transport sensor record",
                    exception
            );
        }
    }

    /*
     * Dựng payload theo thứ tự cố định để thiết bị và backend
     * luôn ký và xác minh cùng một chuỗi byte.
     *
     * batchId ngăn chữ ký được sử dụng cho batch khác.
     * sequence ngăn chữ ký cũ được phát lại trong cùng batch.
     */
    private Map<String, Object> buildUnsignedTransportPayload(
            Batch batch,
            Vehicle vehicle,
            TransportSensorRecordRequest request
    ) {
        Map<String, Object> payload =
                new LinkedHashMap<>();

        /*
         * Không thay đổi thứ tự các field này nếu simulator
         * chưa được sửa tương ứng.
         */
        payload.put("batchId", batch.getId());
        payload.put("vehicleId", vehicle.getId());
        payload.put("sequence", request.getSequence());
        payload.put(
                "vehiclePlate",
                vehicle.getVehiclePlate()
        );
        payload.put(
                "deviceId",
                vehicle.getDeviceId()
        );
        payload.put(
                "sensorFirmware",
                vehicle.getSensorFirmware()
        );
        payload.put("gps", request.getGps());
        payload.put(
                "temperature",
                request.getTemperature()
        );
        payload.put(
                "humidity",
                request.getHumidity()
        );
        payload.put(
                "timestamp",
                request.getTimestamp()
        );

        return payload;
    }

    @Override
    public AdminBatchListResponse DeliveryToDetailer(
            Long batchId,
            Long distributorId
    ) {
        Batch batch = getBatchForDistributor(
                batchId,
                distributorId
        );

        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new IllegalStateException(
                    "Cannot confirm delivery for anchored batch"
            );
        }

        if (batch.getStatus()
                != BatchStatus.IN_DISTRIBUTION) {

            throw new IllegalStateException(
                    "Batch is not in distribution"
            );
        }

        batch.setStatus(
                BatchStatus.DELIVERED_TO_RETAILER
        );

        Batch savedBatch =
                batchRepository.save(batch);

        return toBatchListResponse(savedBatch);
    }

    @Override
    public BatchDetailResponse getBatchDetail(
            Long batchId,
            Long distributorId
    ) {
        Batch batch = getBatchForDistributor(
                batchId,
                distributorId
        );

        return toBatchDetailResponse(batch);
    }

    @Override
    public VerifyAllResponse verifyAllRecords(
            Long batchId,
            Long distributorId
    ) {
        Batch batch = getBatchForDistributor(
                batchId,
                distributorId
        );

        return verifyService.verifyAllRecords(
                batch.getId()
        );
    }

    private Batch getBatchForDistributor(
            Long batchId,
            Long distributorId
    ) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found with id: "
                                        + batchId
                        )
                );

        if (!Objects.equals(
                batch.getDistributorId(),
                distributorId
        )) {
            throw new RuntimeException(
                    "Batch not assigned to this distributor. "
                            + "DB distributorId="
                            + batch.getDistributorId()
                            + ", current distributorId="
                            + distributorId
            );
        }

        return batch;
    }

    private AdminBatchListResponse toBatchListResponse(
            Batch batch
    ) {
        return AdminBatchListResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .createdAt(batch.getCreatedAt())
                .status(String.valueOf(batch.getStatus()))
                .anchorStatus(
                        String.valueOf(batch.getAnchorStatus())
                )
                .recordCount(
                        batch.getRecords() == null
                                ? 0
                                : batch.getRecords().size()
                )
                .build();
    }

    private BatchDetailResponse toBatchDetailResponse(
            Batch batch
    ) {
        return BatchDetailResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .batchCode(batch.getBatchCode())
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .status(batch.getStatus())
                .anchorStatus(batch.getAnchorStatus())
                .createdAt(batch.getCreatedAt())
                .recordCount(
                        batch.getRecords() == null
                                ? 0
                                : batch.getRecords().size()
                )
                .records(
                        batch.getRecords() == null
                                ? List.of()
                                : batch.getRecords()
                                .stream()
                                .map(record ->
                                        RecordItemResponse.builder()
                                                .recordId(record.getId())
                                                .recordKey(record.getRecordKey())
                                                .recordType(record.getRecordType())
                                                .rawJson(record.getRawJson())
                                                .leafHash(record.getLeafHash())
                                                .leafIndex(record.getLeafIndex())
                                                .createdAt(record.getCreatedAt())
                                                .build()
                                )
                                .toList()
                )
                .build();
    }
}