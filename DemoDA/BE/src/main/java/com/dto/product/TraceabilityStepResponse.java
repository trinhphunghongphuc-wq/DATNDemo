package com.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class TraceabilityStepResponse {

    private String recordKey;
    private String rawJson;
    private String leafHash;
    private Integer leafIndex;
    private LocalDateTime createdAt;

    public TraceabilityStepResponse() {
    }

    public TraceabilityStepResponse(String recordKey, String rawJson, String leafHash,
                                    Integer leafIndex, LocalDateTime createdAt) {
        this.recordKey = recordKey;
        this.rawJson = rawJson;
        this.leafHash = leafHash;
        this.leafIndex = leafIndex;
        this.createdAt = createdAt;
    }


}