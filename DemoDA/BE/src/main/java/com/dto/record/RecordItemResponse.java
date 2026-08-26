package com.dto.record;

import com.enums.RecordType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Builder
@Getter
@Setter
public class RecordItemResponse {

    private Long recordId;
    private String recordKey;
    private String rawJson;
    private String leafHash;
    private Integer leafIndex;
    private LocalDateTime createdAt;
    private RecordType recordType;

    public RecordItemResponse() {
    }

    public RecordItemResponse(Long recordId, String recordKey, String rawJson,
                              String leafHash, Integer leafIndex, LocalDateTime createdAt, RecordType recordType) {
        this.recordId = recordId;
        this.recordKey = recordKey;
        this.rawJson = rawJson;
        this.leafHash = leafHash;
        this.leafIndex = leafIndex;
        this.createdAt = createdAt;
        this.recordType = recordType;
    }



}