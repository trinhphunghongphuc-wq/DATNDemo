package com.dto.batch;

import jdk.jshell.Snippet;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class AdminBatchListResponse {

    private Long id;
    private String name;
    private String merkleRoot;
    private String chainTxHash;
    private LocalDateTime createdAt;
    private String status;
    private int recordCount;
    private String anchorStatus;

    public AdminBatchListResponse() {
    }

    public AdminBatchListResponse(Long id, String name, String merkleRoot, String chainTxHash,
                                  LocalDateTime createdAt, String status, int recordCount, String anchorStatus) {
        this.id = id;
        this.name = name;
        this.merkleRoot = merkleRoot;
        this.chainTxHash = chainTxHash;
        this.createdAt = createdAt;
        this.status = status;
        this.recordCount = recordCount;
        this.anchorStatus = anchorStatus;

    }



}