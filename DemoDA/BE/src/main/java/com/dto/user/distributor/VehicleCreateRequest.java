package com.dto.user.distributor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleCreateRequest {

    @NotBlank(message = "Biển số xe không được để trống")
    @Size(max = 50)
    private String vehiclePlate;

//    @NotBlank(message = "Mã thiết bị không được để trống")
//    @Size(max = 100)
//    private String deviceId;

    @NotBlank(message = "Phiên bản firmware không được để trống")
    @Size(max = 100)
    private String sensorFirmware;
}