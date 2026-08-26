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
}