package com.service.impl;

import com.dto.user.distributor.TransportJourneyResponse;
import com.dto.user.distributor.TransportPointResponse;
import com.dto.user.distributor.TransportRouteRequest;
import com.entity.Batch;
import com.entity.Record;
import com.enums.BatchStatus;
import com.enums.RecordType;
import com.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.security.CustomUserDetails;
import com.service.DataEncryptionService;
import com.service.IpfsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class TransportJourneyService {

    private final BatchRepository batches;
    private final RecordRepository records;
    private final IpfsService ipfs;
    private final DataEncryptionService encryption;
    private final ObjectMapper json;
    private final RestClient routes;
    private final String routingUrl;

    public TransportJourneyService(
            BatchRepository batches,
            RecordRepository records,
            IpfsService ipfs,
            DataEncryptionService encryption,
            ObjectMapper json,
            RestClient.Builder restClient,
            @Value("${routing.osrm.url:https://router.project-osrm.org}")
            String routingUrl
    ) {
        this.batches = batches;
        this.records = records;
        this.ipfs = ipfs;
        this.encryption = encryption;
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
            throw new IllegalStateException(
                    "Cannot load transport journey",
                    e
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

    private TransportPointResponse point(
            Long batchId,
            Record record
    ) {
        try {
            String rawJson = record.getRawJson();

            // Transport record riêng tư được lấy từ IPFS và giải mã.
            if (Boolean.TRUE.equals(record.getEncrypted())) {
                if (record.getIpfsCid() == null) {
                    throw new IllegalStateException(
                            "Transport record has no IPFS CID"
                    );
                }

                rawJson = encryption.decrypt(
                        ipfs.getJson(record.getIpfsCid()),
                        "batch:" + batchId
                                + "|record:" + record.getRecordKey()
                );
            }

            JsonNode data = json.readTree(rawJson);
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