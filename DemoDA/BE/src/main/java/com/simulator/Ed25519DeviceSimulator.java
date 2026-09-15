package com.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class Ed25519DeviceSimulator {

    /*
     * Dữ liệu phải trùng với batch và vehicle đang test.
     */
    private static final Long BATCH_ID = 8L;
    private static final Long VEHICLE_ID = 1L;

    /*
     * Request đầu tiên dùng sequence = 1.
     *
     * Sau khi POST thành công:
     * - Vehicle.lastSequence sẽ trở thành 1.
     * - Request tiếp theo phải dùng sequence = 2.
     */
    private static final Long SEQUENCE = 1L;

    private static final String VEHICLE_PLATE =
            "51C-123.45";

    private static final String DEVICE_ID =
            "IOT-DL-001";

    private static final String SENSOR_FIRMWARE =
            "v1.0.0";

    private static final String GPS =
            "11.9404,108.4583";

    private static final Double TEMPERATURE = 7.4;

    private static final Double HUMIDITY = 78.0;

    private static final String TIMESTAMP =
            "2026-09-15T09:30:00+07:00";

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        /*
         * Mô phỏng thiết bị IoT tạo cặp khóa Ed25519.
         *
         * Trong hệ thống thực tế:
         * - Private key chỉ tồn tại trong thiết bị.
         * - Backend chỉ lưu public key.
         */
        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("Ed25519");

        KeyPair keyPair =
                keyPairGenerator.generateKeyPair();

        String publicKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        keyPair.getPublic().getEncoded()
                );

        String privateKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        keyPair.getPrivate().getEncoded()
                );

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * sequence được đưa trực tiếp vào nội dung ký.
         *
         * Vì vậy, kẻ tấn công không thể thay đổi sequence
         * mà vẫn sử dụng lại chữ ký cũ.
         */
        Map<String, Object> signedPayload =
                buildSignedPayload();

        String payloadJson =
                objectMapper.writeValueAsString(
                        signedPayload
                );

        Signature signer =
                Signature.getInstance("Ed25519");

        signer.initSign(keyPair.getPrivate());

        signer.update(
                payloadJson.getBytes(StandardCharsets.UTF_8)
        );

        String deviceSignatureBase64 =
                Base64.getEncoder().encodeToString(
                        signer.sign()
                );

        /*
         * Body gửi lên API chỉ cần những field mà Distributor
         * nhập hoặc thiết bị gửi.
         *
         * Các field vehiclePlate, deviceId, sensorFirmware và
         * batchId sẽ được backend lấy từ database.
         */
        Map<String, Object> postmanBody =
                new LinkedHashMap<>();

        postmanBody.put("vehicleId", VEHICLE_ID);
        postmanBody.put("sequence", SEQUENCE);
        postmanBody.put("gps", GPS);
        postmanBody.put(
                "temperature",
                TEMPERATURE
        );
        postmanBody.put("humidity", HUMIDITY);
        postmanBody.put(
                "timestamp",
                TIMESTAMP
        );
        postmanBody.put(
                "deviceSignature",
                deviceSignatureBase64
        );

        System.out.println(
                "========== PUBLIC KEY =========="
        );
        System.out.println(publicKeyBase64);

        System.out.println();
        System.out.println(
                "========== PRIVATE KEY - KHÔNG ĐƯA LÊN BACKEND =========="
        );
        System.out.println(privateKeyBase64);

        System.out.println();
        System.out.println(
                "========== CHUỖI JSON ĐƯỢC THIẾT BỊ KÝ =========="
        );
        System.out.println(payloadJson);

        System.out.println();
        System.out.println(
                "========== POSTMAN BODY =========="
        );
        System.out.println(
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(postmanBody)
        );

        System.out.println();
        System.out.println(
                "========== SQL ĐĂNG KÝ PUBLIC KEY =========="
        );
        System.out.println(
                "UPDATE vehicles"
                        + " SET device_public_key = '"
                        + publicKeyBase64
                        + "', last_sequence = 0"
                        + " WHERE id = "
                        + VEHICLE_ID
                        + ";"
        );

        System.out.println();
        System.out.println(
                "POST http://localhost:8080/api/distributor/batches/"
                        + BATCH_ID
                        + "/transport-record"
        );
    }

    /*
     * Thứ tự field tại đây phải giống hoàn toàn với
     * buildUnsignedTransportPayload() trong DistributorServiceImpl.
     */
    private static Map<String, Object> buildSignedPayload() {
        Map<String, Object> payload =
                new LinkedHashMap<>();

        payload.put("batchId", BATCH_ID);
        payload.put("vehicleId", VEHICLE_ID);
        payload.put("sequence", SEQUENCE);
        payload.put(
                "vehiclePlate",
                VEHICLE_PLATE
        );
        payload.put("deviceId", DEVICE_ID);
        payload.put(
                "sensorFirmware",
                SENSOR_FIRMWARE
        );
        payload.put("gps", GPS);
        payload.put(
                "temperature",
                TEMPERATURE
        );
        payload.put("humidity", HUMIDITY);
        payload.put("timestamp", TIMESTAMP);

        return payload;
    }
}