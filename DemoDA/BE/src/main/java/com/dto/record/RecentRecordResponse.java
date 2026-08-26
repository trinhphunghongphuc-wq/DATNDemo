package com.dto.record;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentRecordResponse {

    private Long recordId;
    private String recordKey;
    private Long batchId;
    private String batchName;
    private String leafHash;
    private Integer leafIndex;
    private LocalDateTime createdAt;
}