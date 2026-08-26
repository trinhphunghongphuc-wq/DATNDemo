package com.dto.user.distributor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleResponse {

    private Long id;

    private String vehiclePlate;

    private String deviceId;

    private String sensorFirmware;

    private Long distributorId;

    private Boolean active;
}