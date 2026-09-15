package com.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehiclePlate;

    private String deviceId;

    private String sensorFirmware;

    @Column(name = "distributor_id", nullable = false)
    private Long distributorId;

    private Boolean active;

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Lưu public key Ed25519 của thiết bị IoT để backend
     * xác minh nguồn gốc dữ liệu cảm biến.
     *
     * Private key chỉ tồn tại trên thiết bị hoặc chương trình
     * mô phỏng thiết bị, tuyệt đối không lưu trong database.
     *
     * Nullable để tương thích vehicle đã tạo ở Giai đoạn 1.
     */
    @Column(name = "device_public_key", length = 200)
    private String devicePublicKey;


    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Lưu sequence lớn nhất mà backend đã chấp nhận từ thiết bị.
     *
     * Request có sequence cũ hoặc trùng sẽ bị từ chối,
     * dù chữ ký Ed25519 vẫn hợp lệ.
     */
    @Builder.Default
    @Column(name = "last_sequence")
    private Long lastSequence = 0L;
}