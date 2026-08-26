package com.dto.batch;

import com.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchResponse {

    private Long id;
    private String name;
    private String merkleRoot;
    private String chainTxHash;

    public BatchResponse() {
    }

    public BatchResponse(Long id, String name, String merkleRoot, String chainTxHash) {
        this.id = id;
        this.name = name;
        this.merkleRoot = merkleRoot;
        this.chainTxHash = chainTxHash;
    }



}
