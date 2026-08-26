package com.dto.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCategoryResponse {

    private Long id;

    private String name;

    private Double temperatureMin;

    private Double temperatureMax;

    private Double humidityMin;

    private Double humidityMax;

    private String recommendedPackaging;

    private String description;

    private Boolean driedProduct;
}