package com.dto.product;

import com.dto.record.RecordItemResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
public class ProductDetailResponse {

    private Long batchId;
    private String batchName;
    private String productName;
    private String origin;
    private String weight;
    private String merkleRoot;
    private String chainTxHash;
    private LocalDateTime createdAt;
    private String status;
    private List<RecordItemResponse> records;

    public ProductDetailResponse() {
    }

    public ProductDetailResponse(Long batchId, String batchName, String productName, String origin, String weight,
                                 String merkleRoot, String chainTxHash, LocalDateTime createdAt,
                                 String status, List<RecordItemResponse> records) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.productName = productName;
        this.origin = origin;
        this.weight = weight;
        this.merkleRoot = merkleRoot;
        this.chainTxHash = chainTxHash;
        this.createdAt = createdAt;
        this.status = status;
        this.records = records;
    }


}