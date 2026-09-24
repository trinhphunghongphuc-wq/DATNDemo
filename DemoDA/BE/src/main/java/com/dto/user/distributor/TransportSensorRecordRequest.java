package com.dto.user.distributor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransportSensorRecordRequest {

    @NotNull(message = "vehicleId is required")
    private Long vehicleId;

    @NotBlank(message = "gps is required")
    private String gps;

    @NotNull(message = "temperature is required")
    private Double temperature;

    @NotNull(message = "humidity is required")
    private Double humidity;

    @NotBlank(message = "timestamp is required")
    private String timestamp;

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Chữ ký Ed25519 do thiết bị IoT tạo bằng private key.
     *
     * Backend nhận chữ ký Base64 và xác minh bằng
     * devicePublicKey lưu trong Vehicle.
     */
    @NotBlank(message = "deviceSignature is required")
    private String deviceSignature;

    /*
     * Sequence tăng tuần tự trên từng thiết bị.
     * Giá trị này phải được đưa vào payload Ed25519.
     */
    @NotNull(message = "sequence is required")
    @Positive(message = "sequence must be greater than 0")
    private Long sequence;
}