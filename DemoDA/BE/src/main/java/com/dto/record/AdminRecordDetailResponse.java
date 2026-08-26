package com.dto.record;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminRecordDetailResponse {

    private Long id;
    private String recordKey;
    private Long batchId;
    private String batchName;
    private String rawJson;
    private String leafHash;
    private Integer leafIndex;
    private LocalDateTime createdAt;

    public AdminRecordDetailResponse() {
    }

    public AdminRecordDetailResponse(Long id, String recordKey, Long batchId, String batchName,
                                     String rawJson, String leafHash, Integer leafIndex, LocalDateTime createdAt) {
        this.id = id;
        this.recordKey = recordKey;
        this.batchId = batchId;
        this.batchName = batchName;
        this.rawJson = rawJson;
        this.leafHash = leafHash;
        this.leafIndex = leafIndex;
        this.createdAt = createdAt;
    }

}