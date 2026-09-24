package com.dto.batch;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchResponse {

    private Long id;
    private String name;
    private String batchCode;
    private String merkleRoot;
    private String chainTxHash;
    private String qrContent;

    public BatchResponse() {
    }

    public BatchResponse(
            Long id,
            String name,
            String batchCode,
            String merkleRoot,
            String chainTxHash,
            String qrContent
    ) {
        this.id = id;
        this.name = name;
        this.batchCode = batchCode;
        this.merkleRoot = merkleRoot;
        this.chainTxHash = chainTxHash;
        this.qrContent = qrContent;
    }
}