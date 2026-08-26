package com.dto.batch;

import com.enums.AnchorStatus;
import com.enums.BatchStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchListResponse {
    private Long id;
    private String name;
    private String batchCode;
    private String merkleRoot;
    private String chainTxHash;
    private BatchStatus status;
    private AnchorStatus anchorStatus;
    private Integer recordCount;
    private LocalDateTime createdAt;
}