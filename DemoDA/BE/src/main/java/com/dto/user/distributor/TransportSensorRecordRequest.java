package com.dto.user.distributor;

import lombok.Data;

@Data
public class TransportSensorRecordRequest {

    private Long vehicleId;

    private String gps;

    private Double temperature;

    private Double humidity;

    private String timestamp;
}