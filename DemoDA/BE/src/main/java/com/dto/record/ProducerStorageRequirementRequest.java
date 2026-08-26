package com.dto.record;

import lombok.Data;

@Data
public class ProducerStorageRequirementRequest {

    private Double temperatureMin;

    private Double temperatureMax;

    private Double humidityMin;

    private Double humidityMax;

    private String note;
}