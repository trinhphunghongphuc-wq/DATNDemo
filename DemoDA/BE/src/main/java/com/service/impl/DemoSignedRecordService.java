package com.service.impl;

import com.controller.DemoSignedRecordController.PrepareRequest;
import com.entity.Batch;
import com.entity.Vehicle;
import com.enums.BatchStatus;
import com.enums.RecordStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.StageAnchorTransactionRepository;
import com.repository.VehicleRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Chỉ dùng để kiểm thử tại máy local: khóa riêng ở bộ nhớ BE, không gửi tới browser. */
@Service
@ConditionalOnProperty(name = "app.demo-device.enabled", havingValue = "true")
public class DemoSignedRecordService {
    private final BatchRepository batches;
    private final VehicleRepository vehicles;
    private final StageAnchorTransactionRepository anchors;
    private final ObjectMapper json;
    private final Map<Long, KeyPair> keys = new ConcurrentHashMap<>();

    public DemoSignedRecordService(BatchRepository batches, VehicleRepository vehicles,
                                   StageAnchorTransactionRepository anchors, ObjectMapper json) {
        this.batches = batches;
        this.vehicles = vehicles;
        this.anchors = anchors;
        this.json = json;
    }

    @Transactional
    public Map<String, Object> prepare(Long batchId, Long distributorId, PrepareRequest request) {
        Batch batch = batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));
        if (!Objects.equals(batch.getDistributorId(), distributorId)) {
            throw new IllegalArgumentException("Batch does not belong to this distributor");
        }
        if (batch.getStatus() != BatchStatus.RECEIVED_BY_DISTRIBUTOR
                && batch.getStatus() != BatchStatus.IN_DISTRIBUTION) {
            throw new IllegalStateException("Batch is not accepting TRANSPORT records");
        }
        if (anchors.existsByBatch_IdAndRecordStage(batchId, RecordStage.DISTRIBUTOR)) {
            throw new IllegalStateException("DISTRIBUTOR stage is already anchored");
        }
        String[] coords = request.gps().split(",", -1);
        if (coords.length != 2) throw new IllegalArgumentException("GPS must be lat,lon");
        try {
            double lat = Double.parseDouble(coords[0].trim());
            double lon = Double.parseDouble(coords[1].trim());
            if (!Double.isFinite(lat) || !Double.isFinite(lon)
                    || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid GPS coordinates", ex);
        }
        if (!Double.isFinite(request.temperature()) || !Double.isFinite(request.humidity())) {
            throw new IllegalArgumentException("Temperature and humidity must be finite");
        }

        // Khóa hàng xe để tránh tranh chấp với request ghi điểm đo. Chỉ gọi khi xe rảnh.
        Vehicle vehicle = vehicles.findActiveVehicleForUpdate(request.vehicleId(), distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        KeyPair pair = keys.get(vehicle.getId());
        if (pair == null) {
            try {
                pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot initialize demo key", ex);
            }
            // Chế độ demo thay public key của xe, giữ nguyên lastSequence.
            vehicle.setDevicePublicKey(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
            vehicles.save(vehicle);
            keys.put(vehicle.getId(), pair);
        } else {
            String current = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            if (!current.equals(vehicle.getDevicePublicKey())) {
                throw new IllegalStateException("Vehicle key was changed; restart demo signing session");
            }
        }

        long sequence = (vehicle.getLastSequence() == null ? 0L : vehicle.getLastSequence()) + 1;
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        // Chính xác thứ tự và kiểu dữ liệu trong DistributorServiceImpl.buildUnsignedTransportPayload.
        payload.put("batchId", batch.getId());
        payload.put("vehicleId", vehicle.getId());
        payload.put("sequence", sequence);
        payload.put("vehiclePlate", vehicle.getVehiclePlate());
        payload.put("deviceId", vehicle.getDeviceId());
        payload.put("sensorFirmware", vehicle.getSensorFirmware());
        payload.put("gps", request.gps());
        payload.put("temperature", request.temperature());
        payload.put("humidity", request.humidity());
        payload.put("timestamp", timestamp);

        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(pair.getPrivate());
            signer.update(json.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("vehicleId", vehicle.getId());
            result.put("sequence", sequence);
            result.put("gps", request.gps());
            result.put("temperature", request.temperature());
            result.put("humidity", request.humidity());
            result.put("timestamp", timestamp);
            result.put("deviceSignature", Base64.getEncoder().encodeToString(signer.sign()));
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign demo record", ex);
        }
    }
}
