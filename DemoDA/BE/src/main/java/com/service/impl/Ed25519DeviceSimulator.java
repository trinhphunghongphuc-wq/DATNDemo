package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@Service
public class Ed25519DeviceSimulator {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "show-key".equals(args[0])) {
            showPublicKey(Path.of(args[1]));
        } else if (args.length == 3 && "init".equals(args[0])) {
            init(Long.parseLong(args[1]), Path.of(args[2]));
        } else if (args.length == 12 && "sign".equals(args[0])) {
            sign(args);
        } else if (args.length == 14 && "trip".equals(args[0])) {
            trip(args);
        } else {
            System.err.println("Usage:");
            System.err.println("  show-key <privateKeyFile>");
            System.err.println("  init <vehicleId> <privateKeyFile>");
            System.err.println(
                    "  sign <privateKeyFile> <batchId> <vehicleId> <sequence>"
                            + " <vehiclePlate> <deviceId> <sensorFirmware>"
                            + " <lat,lon> <temperature> <humidity>"
                            + " <ISO-offset-timestamp>"
            );
            System.err.println(
                    "  trip <privateKeyFile> <batchId> <vehicleId> <nextSequence>"
                            + " <vehiclePlate> <deviceId> <sensorFirmware>"
                            + " <originLat,originLon>"
                            + " <destinationLat,destinationLon>"
                            + " <temperature> <humidity>"
                            + " <pointCount> <intervalSeconds>"
            );
            System.exit(2);
        }
    }

    private static void init(long vehicleId, Path keyFile)
            throws Exception {
        if (vehicleId <= 0) {
            throw new IllegalArgumentException(
                    "vehicleId must be positive"
            );
        }

        KeyPair pair = KeyPairGenerator
                .getInstance("Ed25519")
                .generateKeyPair();

        Files.createDirectories(
                keyFile.toAbsolutePath().getParent()
        );

        String encoded = Base64.getEncoder()
                .encodeToString(pair.getPrivate().getEncoded());

        Files.writeString(
                keyFile,
                encoded,
                StandardCharsets.US_ASCII,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        try {
            Files.setPosixFilePermissions(
                    keyFile,
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE
                    )
            );
        } catch (UnsupportedOperationException ignored) {
            // Windows dùng cơ chế phân quyền file riêng.
        }

        String publicKey = Base64.getEncoder()
                .encodeToString(pair.getPublic().getEncoded());

        System.out.println(
                "Public key (Base64 X.509): " + publicKey
        );
        System.out.println(
                "Register once, without resetting last_sequence:"
        );
        System.out.println(
                "UPDATE vehicles SET device_public_key = '"
                        + publicKey
                        + "' WHERE id = "
                        + vehicleId
                        + ";"
        );
        System.out.println(
                "Private key saved to "
                        + keyFile.toAbsolutePath()
        );
    }

    private static void sign(String[] args) throws Exception {
        Path keyFile = Path.of(args[1]);
        long batchId = Long.parseLong(args[2]);
        long vehicleId = Long.parseLong(args[3]);
        long sequence = Long.parseLong(args[4]);

        if (batchId <= 0 || vehicleId <= 0 || sequence <= 0) {
            throw new IllegalArgumentException(
                    "IDs and sequence must be positive"
            );
        }

        String vehiclePlate = args[5];
        String deviceId = args[6];
        String firmware = args[7];
        String gps = args[8];

        validateGps(gps);

        double temperature = Double.parseDouble(args[9]);
        double humidity = Double.parseDouble(args[10]);

        if (!Double.isFinite(temperature)
                || !Double.isFinite(humidity)
                || humidity < 0
                || humidity > 100) {
            throw new IllegalArgumentException(
                    "Invalid temperature or humidity"
            );
        }

        String timestamp = args[11];
        OffsetDateTime.parse(timestamp);

        Map<String, Object> request = signedRequest(
                readKey(keyFile),
                batchId,
                vehicleId,
                sequence,
                vehiclePlate,
                deviceId,
                firmware,
                gps,
                temperature,
                humidity,
                timestamp
        );

        System.out.println(
                "POST http://localhost:8080/api/distributor/batches/"
                        + batchId
                        + "/transport-record"
        );
        System.out.println(
                JSON.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request)
        );
    }

    private static PrivateKey readKey(Path file)
            throws Exception {
        byte[] bytes = Base64.getDecoder().decode(
                Files.readString(
                        file,
                        StandardCharsets.US_ASCII
                ).trim()
        );

        return KeyFactory.getInstance("Ed25519")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(bytes)
                );
    }

    private static void showPublicKey(Path keyFile) throws Exception {
        java.security.interfaces.EdECPrivateKey privateKey =
                (java.security.interfaces.EdECPrivateKey) readKey(keyFile);

        byte[] seed = privateKey.getBytes()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot read Ed25519 private key bytes"
                ));

        byte[] rawPublicKey =
                new org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(
                        seed, 0
                ).generatePublicKey().getEncoded();

        // Phần đầu X.509 SubjectPublicKeyInfo dành cho Ed25519.
        byte[] x509Prefix = java.util.HexFormat.of()
                .parseHex("302a300506032b6570032100");

        byte[] x509PublicKey =
                new byte[x509Prefix.length + rawPublicKey.length];

        System.arraycopy(
                x509Prefix, 0,
                x509PublicKey, 0,
                x509Prefix.length
        );
        System.arraycopy(
                rawPublicKey, 0,
                x509PublicKey, x509Prefix.length,
                rawPublicKey.length
        );

        System.out.println(
                "Public key (Base64 X.509): "
                        + Base64.getEncoder().encodeToString(x509PublicKey)
        );
    }

    private static Map<String, Object> signedRequest(
            PrivateKey privateKey,
            long batchId,
            long vehicleId,
            long sequence,
            String vehiclePlate,
            String deviceId,
            String firmware,
            String gps,
            double temperature,
            double humidity,
            String timestamp
    ) throws Exception {
        // Đúng thứ tự DistributorServiceImpl.buildUnsignedTransportPayload().
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("batchId", batchId);
        payload.put("vehicleId", vehicleId);
        payload.put("sequence", sequence);
        payload.put("vehiclePlate", vehiclePlate);
        payload.put("deviceId", deviceId);
        payload.put("sensorFirmware", firmware);
        payload.put("gps", gps);
        payload.put("temperature", temperature);
        payload.put("humidity", humidity);
        payload.put("timestamp", timestamp);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(
                JSON.writeValueAsString(payload)
                        .getBytes(StandardCharsets.UTF_8)
        );

        String signature = Base64.getEncoder()
                .encodeToString(signer.sign());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("vehicleId", vehicleId);
        request.put("sequence", sequence);
        request.put("gps", gps);
        request.put("temperature", temperature);
        request.put("humidity", humidity);
        request.put("timestamp", timestamp);
        request.put("deviceSignature", signature);

        return request;
    }

    private static void trip(String[] args) throws Exception {
        String token = System.getenv("SIMULATOR_JWT");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Set SIMULATOR_JWT to a Distributor token"
            );
        }

        String api = System.getenv().getOrDefault(
                "SIMULATOR_API_URL",
                "http://localhost:8080"
        );
        api = api.replaceAll("/+$", "");

        PrivateKey key = readKey(Path.of(args[1]));
        long batchId = Long.parseLong(args[2]);
        long vehicleId = Long.parseLong(args[3]);
        long nextSequence = Long.parseLong(args[4]);

        if (batchId <= 0
                || vehicleId <= 0
                || nextSequence <= 0) {
            throw new IllegalArgumentException(
                    "IDs and sequence must be positive"
            );
        }

        String plate = args[5];
        String deviceId = args[6];
        String firmware = args[7];
        String origin = args[8];
        String destination = args[9];

        validateGps(origin);
        validateGps(destination);

        double temperature = Double.parseDouble(args[10]);
        double humidity = Double.parseDouble(args[11]);

        if (!Double.isFinite(temperature)
                || !Double.isFinite(humidity)
                || humidity < 0
                || humidity > 100) {
            throw new IllegalArgumentException(
                    "Invalid temperature or humidity"
            );
        }

        int pointCount = Integer.parseInt(args[12]);
        int intervalSeconds = Integer.parseInt(args[13]);

        if (pointCount < 2
                || pointCount > 100
                || intervalSeconds < 0) {
            throw new IllegalArgumentException(
                    "Use 2..100 points and a nonnegative interval"
            );
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        URI journeyUri = URI.create(
                api + "/api/transport/batches/"
                        + batchId
                        + "/journey"
        );

        JsonNode journey = request(
                client,
                journeyUri,
                "GET",
                null,
                token
        );

        JsonNode geometry = journey.path("plannedRoute");

        if (geometry.isMissingNode() || geometry.isNull()) {
            String[] start = origin.split(",");
            String[] end = destination.split(",");

            Map<String, Double> routeRequest =
                    new LinkedHashMap<>();

            routeRequest.put(
                    "originLat",
                    Double.parseDouble(start[0])
            );
            routeRequest.put(
                    "originLon",
                    Double.parseDouble(start[1])
            );
            routeRequest.put(
                    "destinationLat",
                    Double.parseDouble(end[0])
            );
            routeRequest.put(
                    "destinationLon",
                    Double.parseDouble(end[1])
            );

            journey = request(
                    client,
                    URI.create(
                            api + "/api/transport/batches/"
                                    + batchId
                                    + "/route"
                    ),
                    "POST",
                    JSON.writeValueAsString(routeRequest),
                    token
            );

            geometry = journey.path("plannedRoute");
        }

        // GeoJSON chứa [longitude, latitude].
        JsonNode coordinates =
                geometry.path("coordinates");

        if (!coordinates.isArray()
                || coordinates.size() < 2) {
            throw new IllegalStateException(
                    "Planned route has no coordinates"
            );
        }

        if (pointCount > coordinates.size()) {
            throw new IllegalArgumentException(
                    "Route has only "
                            + coordinates.size()
                            + " coordinates; reduce pointCount"
            );
        }

        for (int i = 0; i < pointCount; i++) {
            int index = (int) Math.round(
                    (double) i
                            * (coordinates.size() - 1)
                            / (pointCount - 1)
            );

            JsonNode point = coordinates.get(index);

            String gps = String.format(
                    Locale.ROOT,
                    "%.6f,%.6f",
                    point.get(1).asDouble(),
                    point.get(0).asDouble()
            );

            long sequence = nextSequence + i;

            Map<String, Object> body = signedRequest(
                    key,
                    batchId,
                    vehicleId,
                    sequence,
                    plate,
                    deviceId,
                    firmware,
                    gps,
                    temperature,
                    humidity,
                    OffsetDateTime.now(ZoneOffset.UTC)
                            .toString()
            );

            request(
                    client,
                    URI.create(
                            api
                                    + "/api/distributor/batches/"
                                    + batchId
                                    + "/transport-record"
                    ),
                    "POST",
                    JSON.writeValueAsString(body),
                    token
            );

            System.out.println(
                    "Accepted GPS point "
                            + (i + 1)
                            + "/"
                            + pointCount
                            + ", sequence="
                            + sequence
                            + ", gps="
                            + gps
            );

            if (i + 1 < pointCount) {
                Thread.sleep(intervalSeconds * 1000L);
            }
        }

        System.out.println(
                "Trip completed. Next vehicle sequence: "
                        + (nextSequence + pointCount)
        );
    }

    private static JsonNode request(
            HttpClient client,
            URI uri,
            String method,
            String body,
            String token
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(45))
                .header(
                        "Authorization",
                        "Bearer " + token
                )
                .header("Accept", "application/json");

        if ("POST".equals(method)) {
            builder.header(
                    "Content-Type",
                    "application/json"
            ).POST(
                    HttpRequest.BodyPublishers.ofString(body)
            );
        } else {
            builder.GET();
        }

        HttpResponse<String> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    method + " "
                            + uri
                            + " returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return JSON.readTree(response.body());
    }

    private static void validateGps(String gps) {
        String[] coordinates = gps.split(",", -1);

        if (coordinates.length != 2) {
            throw new IllegalArgumentException(
                    "GPS must be lat,lon"
            );
        }

        double latitude =
                Double.parseDouble(coordinates[0]);
        double longitude =
                Double.parseDouble(coordinates[1]);

        if (!Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180) {
            throw new IllegalArgumentException(
                    "GPS coordinates out of range"
            );
        }
    }
}