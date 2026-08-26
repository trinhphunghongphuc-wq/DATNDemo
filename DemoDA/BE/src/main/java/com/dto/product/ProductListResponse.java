package com.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListResponse {

    private Long batchId;
    private String productName;
    private String origin;
    private String weight;
    private String status;

    public ProductListResponse() {
    }

    public ProductListResponse(Long batchId, String productName, String origin, String weight, String status) {
        this.batchId = batchId;
        this.productName = productName;
        this.origin = origin;
        this.weight = weight;
        this.status = status;
    }

}