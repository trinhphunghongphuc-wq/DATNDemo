package com.service.impl;

import com.dto.user.distributor.TransportJourneyResponse;
import com.dto.user.distributor.TransportPointResponse;
import com.dto.user.distributor.TransportRouteRequest;
import com.dto.user.distributor.TransportSignatureEvidenceResponse;
import com.entity.Batch;
import com.entity.Record;
import com.enums.BatchStatus;
import com.enums.RecordType;
import com.enums.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.security.CustomUserDetails;
import com.service.DataEncryptionService;
import com.service.DeviceSignatureService;
import com.service.IpfsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
public class TransportJourneyService {

    private final BatchRepository batches;
    private final RecordRepository records;
    private final IpfsService ipfs;
    private final DataEncryptionService encryption;
    private final DeviceSignatureService deviceSignatureService;
    private final ObjectMapper json;
    private final RestClient routes;
    private final String routingUrl;

    public TransportJourneyService(
            BatchRepository batches,
            RecordRepository records,
            IpfsService ipfs,
            DataEncryptionService encryption,
            DeviceSignatureService deviceSignatureService,
            ObjectMapper json,
            RestClient.Builder restClient,
            @Value("${routing.osrm.url:https://router.project-osrm.org}")
            String routingUrl
    ) {
        this.batches = batches;
        this.records = records;
        this.ipfs = ipfs;
        this.encryption = encryption;
        this.deviceSignatureService = deviceSignatureService;
        this.json = json;
        this.routes = restClient.build();
        this.routingUrl = routingUrl.replaceAll("/+$", "");
    }

    @Transactional
    public TransportJourneyResponse plan(
            Long batchId,
            TransportRouteRequest request,
            CustomUserDetails user
    ) {
        Batch batch = permittedBatch(batchId, user);

        if (user.getRole() != Role.DISTRIBUTOR) {
            throw new AccessDeniedException(
                    "Only the assigned distributor can plan a route"
            );
        }

        if (batch.getStatus() != BatchStatus.RECEIVED_BY_DISTRIBUTOR) {
            throw new IllegalStateException(
                    "Receive the batch before planning its route"
            );
        }

        if (batch.getTransportRouteGeoJson() != null) {
            throw new IllegalStateException(
                    "Route has already been planned for this batch"
            );
        }

        if (!records.findByBatchIdAndRecordTypeOrderByStageLeafIndexAsc(
                batchId, RecordType.TRANSPORT
        ).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot change route after sensor readings"
            );
        }

        // OSRM nhận tọa độ theo thứ tự longitude,latitude.
        String coordinates = String.format(
                Locale.ROOT,
                "%.6f,%.6f;%.6f,%.6f",
                request.originLon(),
                request.originLat(),
                request.destinationLon(),
                request.destinationLat()
        );

        URI uri = URI.create(
                routingUrl
                        + "/route/v1/driving/"
                        + coordinates
                        + "?overview=full&geometries=geojson"
        );

        JsonNode result = routes.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode.class);

        JsonNode geometry = result == null
                ? null
                : result.path("routes").path(0).path("geometry");

        if (geometry == null
                || !"Ok".equals(result.path("code").asText())
                || !"LineString".equals(geometry.path("type").asText())
                || geometry.path("coordinates").size() < 2) {
            throw new IllegalStateException(
                    "Routing service did not return a road route"
            );
        }

        batch.setTransportRouteGeoJson(geometry.toString());
        batches.save(batch);

        return new TransportJourneyResponse(
                batchId,
                geometry,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public TransportJourneyResponse get(
            Long batchId,
            CustomUserDetails user
    ) {
        Batch batch = permittedBatch(batchId, user);

        try {
            JsonNode planned = batch.getTransportRouteGeoJson() == null
                    ? null
                    : json.readTree(batch.getTransportRouteGeoJson());

            List<TransportPointResponse> points = records
                    .findByBatchIdAndRecordTypeOrderByStageLeafIndexAsc(
                            batchId,
                            RecordType.TRANSPORT
                    )
                    .stream()
                    .map(record -> point(batchId, record))
                    .toList();

            return new TransportJourneyResponse(
                    batchId,
                    planned,
                    points
            );
        } catch (Exception e) {
            log.error(
                    "Cannot load transport journey for batchId={}",
                    batchId,
                    e
            );

            throw new IllegalStateException(
                    "Cannot load transport journey",
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public TransportSignatureEvidenceResponse signatureEvidence(
            Long batchId,
            String recordKey,
            CustomUserDetails user
    ) {
        // Phải kiểm tra quyền trước khi đọc dữ liệu riêng tư từ IPFS.
        permittedBatch(batchId, user);

        Record record = records
                .findByBatchIdAndRecordKey(batchId, recordKey)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy record trong batch này"
                        )
                );

        if (record.getRecordType() != RecordType.TRANSPORT) {
            throw new IllegalArgumentException(
                    "Record này không phải điểm đo vận chuyển"
            );
        }

        try {
            String rawJson = readTransportJson(batchId, record);

            // Giữ thứ tự field đúng như JSON mà thiết bị đã ký.
            LinkedHashMap<String, Object> signedPayload = json.readValue(
                    rawJson,
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );

            String signature = (String) signedPayload.remove(
                    "deviceSignature"
            );
            String publicKey = (String) signedPayload.remove(
                    "devicePublicKeyAtSigning"
            );

            Number payloadBatchId = (Number) signedPayload.get("batchId");
            Number vehicleId = (Number) signedPayload.get("vehicleId");
            Number sequence = (Number) signedPayload.get("sequence");

            if (payloadBatchId == null
                    || payloadBatchId.longValue() != batchId.longValue()
                    || vehicleId == null
                    || sequence == null
                    || signature == null
                    || signature.isBlank()) {
                throw new IllegalStateException(
                        "Dữ liệu bằng chứng trong record không hợp lệ"
                );
            }

            String deviceId = (String) signedPayload.get("deviceId");
            String timestamp = (String) signedPayload.get("timestamp");

            if (publicKey == null || publicKey.isBlank()) {
                return new TransportSignatureEvidenceResponse(
                        recordKey,
                        batchId,
                        vehicleId.longValue(),
                        deviceId,
                        sequence.longValue(),
                        timestamp,
                        null,
                        null,
                        "Record cũ chưa lưu public key tại thời điểm nhận "
                                + "điểm đo; không thể kiểm tra lại chữ ký."
                );
            }

            String signedJson = json.writeValueAsString(signedPayload);

            boolean valid = deviceSignatureService.verifySignature(
                    signedJson,
                    signature,
                    publicKey
            );

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
            String fingerprint = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(publicKeyBytes)
            );

            return new TransportSignatureEvidenceResponse(
                    recordKey,
                    batchId,
                    vehicleId.longValue(),
                    deviceId,
                    sequence.longValue(),
                    timestamp,
                    fingerprint,
                    valid,
                    valid
                            ? "Chữ ký thiết bị hợp lệ"
                            : "Chữ ký thiết bị không hợp lệ"
            );
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "Cannot verify transport signature for recordKey={}",
                    recordKey,
                    exception
            );

            throw new IllegalStateException(
                    "Không đọc hoặc kiểm tra được bằng chứng điểm đo",
                    exception
            );
        }
    }

    private Batch permittedBatch(
            Long batchId,
            CustomUserDetails user
    ) {
        Batch batch = batches.findById(batchId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Batch not found: " + batchId
                        )
                );

        boolean allowed =
                user.getRole() == Role.ADMIN
                        || user.getRole() == Role.DISTRIBUTOR
                        && Objects.equals(
                        batch.getDistributorId(),
                        user.getId()
                )
                        || user.getRole() == Role.RETAILER
                        && Objects.equals(
                        batch.getRetailerId(),
                        user.getId()
                );

        if (!allowed) {
            throw new AccessDeniedException(
                    "Batch does not belong to this user"
            );
        }

        return batch;
    }

    private String readTransportJson(Long batchId, Record record) {
        if (!Boolean.TRUE.equals(record.getEncrypted())) {
            return record.getRawJson();
        }

        if (record.getIpfsCid() == null
                || record.getIpfsCid().isBlank()) {
            throw new IllegalStateException(
                    "Transport record không có IPFS CID"
            );
        }

        return encryption.decrypt(
                ipfs.getJson(record.getIpfsCid()),
                "batch:" + batchId
                        + "|record:" + record.getRecordKey()
        );
    }

    private TransportPointResponse point(
            Long batchId,
            Record record
    ) {
        try {
            JsonNode data = json.readTree(
                    readTransportJson(batchId, record)
            );

            String[] gps = data.path("gps").asText().split(",", -1);

            if (gps.length != 2) {
                throw new IllegalStateException(
                        "Invalid GPS in record"
                );
            }

            return new TransportPointResponse(
                    record.getRecordKey(),
                    data.path("vehicleId").asLong(),
                    data.path("sequence").asLong(),
                    Double.parseDouble(gps[0]),
                    Double.parseDouble(gps[1]),
                    data.path("temperature").asDouble(),
                    data.path("humidity").asDouble(),
                    data.path("timestamp").asText()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot read transport record "
                            + record.getRecordKey(),
                    e
            );
        }
    }
}