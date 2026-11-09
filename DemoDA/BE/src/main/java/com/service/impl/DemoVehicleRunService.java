package com.service.impl;

import com.dto.user.distributor.DemoRunRequest;
import com.dto.user.distributor.DemoRunResponse;
import com.dto.user.distributor.TransportSensorRecordRequest;
import com.entity.Batch;
import com.entity.Vehicle;
import com.enums.BatchStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.VehicleRepository;
import com.service.DistributorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.demo-device.enabled",
        havingValue = "true"
)
public class DemoVehicleRunService {
    private final BatchRepository batchRepository;
    private final VehicleRepository vehicleRepository;
    private final DistributorService distributorService;
    private final DemoVehicleRunRegistry registry;
    private final ObjectMapper objectMapper;

    private final Map<Long, Run> runs = new ConcurrentHashMap<>();

    private static class Run {
        final Long batchId;
        final Long distributorId;
        final Vehicle vehicle;
        final KeyPair keyPair;
        final List<double[]> route;
        final int total;
        final double temperature;
        final double humidity;
        final long firstSequence;
        final AtomicBoolean sending = new AtomicBoolean(false);

        volatile int sent = 0;
        volatile String status = "RUNNING";
        volatile String error;

        Run(
                Long batchId,
                Long distributorId,
                Vehicle vehicle,
                KeyPair keyPair,
                List<double[]> route,
                DemoRunRequest request,
                long firstSequence
        ) {
            this.batchId = batchId;
            this.distributorId = distributorId;
            this.vehicle = vehicle;
            this.keyPair = keyPair;
            this.route = route;
            this.total = request.pointCount();
            this.temperature = request.temperature();
            this.humidity = request.humidity();
            this.firstSequence = firstSequence;
        }

        DemoRunResponse response() {
            return new DemoRunResponse(
                    batchId, vehicle.getId(), status, sent, total, error
            );
        }
    }

    @Transactional
    public synchronized DemoRunResponse start(
            Long batchId,
            Long distributorId,
            DemoRunRequest request
    ) {
        Batch batch = ownedBatch(batchId, distributorId);

        Run previous = runs.get(batchId);
        if (previous != null && (
                previous.status.equals("RUNNING")
                        || previous.status.equals("STOPPING")
        )) {
            // Bấm Start hai lần hoặc mở lại trang không tạo thêm phiên.
            return previous.response();
        }

        if (batch.getStatus() != BatchStatus.RECEIVED_BY_DISTRIBUTOR
                && batch.getStatus() != BatchStatus.IN_DISTRIBUTION) {
            throw new IllegalStateException(
                    "Lô phải được Distributor nhận và chưa giao cho Retailer"
            );
        }

        if (request.pointCount() == null
                || request.pointCount() < 2
                || request.pointCount() > 100
                || request.temperature() == null
                || !Double.isFinite(request.temperature())
                || request.humidity() == null
                || !Double.isFinite(request.humidity())
                || request.humidity() < 0
                || request.humidity() > 100) {
            throw new IllegalArgumentException("Thông số demo không hợp lệ");
        }

        List<double[]> route = readRoute(batch);
        if (request.pointCount() > route.size()) {
            throw new IllegalArgumentException(
                    "Số điểm demo vượt quá số tọa độ trên tuyến"
            );
        }

        registry.reserve(request.vehicleId(), batchId);

        try {
            Vehicle vehicle = vehicleRepository
                    .findActiveVehicleForUpdate(
                            request.vehicleId(), distributorId
                    )
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Xe không tồn tại hoặc không thuộc Distributor"
                    ));

            KeyPair keyPair = KeyPairGenerator
                    .getInstance("Ed25519")
                    .generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(
                    keyPair.getPublic().getEncoded()
            );
            vehicle.setDevicePublicKey(publicKey);
            vehicleRepository.save(vehicle);

            long lastSequence = vehicle.getLastSequence() == null
                    ? 0L : vehicle.getLastSequence();

            Run run = new Run(
                    batchId, distributorId, vehicle, keyPair,
                    route, request, lastSequence + 1
            );
            runs.put(batchId, run);
            return run.response();
        } catch (Exception exception) {
            registry.release(request.vehicleId(), batchId);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(
                    "Không tạo được khóa demo Ed25519", exception
            );
        }
    }

    public DemoRunResponse status(Long batchId, Long distributorId) {
        ownedBatch(batchId, distributorId);
        Run run = runs.get(batchId);
        return run == null
                ? new DemoRunResponse(batchId, null, "IDLE", 0, 0, null)
                : run.response();
    }

    public DemoRunResponse stop(Long batchId, Long distributorId) {
        ownedBatch(batchId, distributorId);
        Run run = runs.get(batchId);

        if (run == null) {
            return new DemoRunResponse(
                    batchId, null, "IDLE", 0, 0, null
            );
        }

        if (run.status.equals("RUNNING")) {
            if (run.sending.get()) {
                // Điểm đang upload được hoàn tất trước khi nhả xe.
                run.status = "STOPPING";
            } else {
                run.status = "STOPPED";
                registry.release(run.vehicle.getId(), batchId);
            }
        }
        return run.response();
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 15000)
    public void sendNextPoints() {
        for (Run run : runs.values()) {
            sendOne(run);
        }
    }

    private void sendOne(Run run) {
        if (!run.status.equals("RUNNING")
                || !run.sending.compareAndSet(false, true)) {
            return;
        }

        try {
            if (!run.status.equals("RUNNING")) {
                return;
            }

            int index = run.sent;
            int routeIndex = (int) Math.round(
                    index * (run.route.size() - 1.0)
                            / (run.total - 1.0)
            );
            double[] position = run.route.get(routeIndex);

            // Dữ liệu GPS hiện tại dùng thứ tự latitude,longitude.
            String gps = String.format(
                    Locale.ROOT, "%.6f,%.6f",
                    position[1], position[0]
            );
            String timestamp = Instant.now().toString();
            long sequence = run.firstSequence + index;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("batchId", run.batchId);
            payload.put("vehicleId", run.vehicle.getId());
            payload.put("sequence", sequence);
            payload.put("vehiclePlate", run.vehicle.getVehiclePlate());
            payload.put("deviceId", run.vehicle.getDeviceId());
            payload.put("sensorFirmware", run.vehicle.getSensorFirmware());
            payload.put("gps", gps);
            payload.put("temperature", run.temperature);
            payload.put("humidity", run.humidity);
            payload.put("timestamp", timestamp);

            String unsignedJson = objectMapper.writeValueAsString(payload);
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(run.keyPair.getPrivate());
            signer.update(unsignedJson.getBytes(StandardCharsets.UTF_8));

            TransportSensorRecordRequest request =
                    new TransportSensorRecordRequest();
            request.setVehicleId(run.vehicle.getId());
            request.setSequence(sequence);
            request.setGps(gps);
            request.setTemperature(run.temperature);
            request.setHumidity(run.humidity);
            request.setTimestamp(timestamp);
            request.setDeviceSignature(
                    Base64.getEncoder().encodeToString(signer.sign())
            );

            registry.runAsDemoVehicle(
                    run.vehicle.getId(),
                    () -> distributorService.addTransportRecord(
                            run.batchId, request, run.distributorId
                    )
            );

            // Chỉ tăng sau khi BE đã lưu record thành công.
            run.sent = index + 1;

            if (run.sent >= run.total) {
                run.status = "COMPLETED";
                registry.release(run.vehicle.getId(), run.batchId);
            }
        } catch (Exception exception) {
            run.error = exception.getMessage();
            run.status = "FAILED";
            registry.release(run.vehicle.getId(), run.batchId);
        } finally {
            run.sending.set(false);
            if (run.status.equals("STOPPING")) {
                run.status = "STOPPED";
                registry.release(run.vehicle.getId(), run.batchId);
            }
        }
    }

    private Batch ownedBatch(Long batchId, Long distributorId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy lô")
                );
        if (!Objects.equals(batch.getDistributorId(), distributorId)) {
            throw new IllegalArgumentException(
                    "Lô không thuộc Distributor hiện tại"
            );
        }
        return batch;
    }

    private List<double[]> readRoute(Batch batch) {
        try {
            if (batch.getTransportRouteGeoJson() == null) {
                throw new IllegalStateException("Lô chưa có tuyến đường");
            }

            JsonNode root = objectMapper.readTree(
                    batch.getTransportRouteGeoJson()
            );
            if (!"LineString".equals(root.path("type").asText())) {
                throw new IllegalStateException("Tuyến đường không hợp lệ");
            }

            List<double[]> route = new ArrayList<>();
            for (JsonNode coordinate : root.path("coordinates")) {
                if (coordinate.isArray() && coordinate.size() >= 2) {
                    route.add(new double[]{
                            coordinate.get(0).asDouble(),
                            coordinate.get(1).asDouble()
                    });
                }
            }
            if (route.size() < 2) {
                throw new IllegalStateException("Tuyến đường quá ngắn");
            }
            return route;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Không đọc được tuyến đường", exception
            );
        }
    }
}