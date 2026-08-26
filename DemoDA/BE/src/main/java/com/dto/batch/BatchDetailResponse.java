package com.dto.batch;

import com.dto.record.RecordItemResponse;
import com.enums.AnchorStatus;
import com.enums.BatchStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDetailResponse {

    private Long id;
    private String name;
    private String batchCode;
    private String merkleRoot;
    private String chainTxHash;
    private BatchStatus status;
    private AnchorStatus anchorStatus;
    private LocalDateTime createdAt;

    private Integer recordCount;

    private List<RecordItemResponse> records;
}