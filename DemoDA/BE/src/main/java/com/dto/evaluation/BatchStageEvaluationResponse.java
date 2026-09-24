package com.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@Builder
public class BatchStageEvaluationResponse {

    private Long batchId;

    // Tổng số record thật của batch.
    private Integer totalRecords;

    // Số stage đang có record.
    private Integer stagesWithRecords;

    // Nếu mỗi record dùng một transaction.
    private Integer hashPerRecordTransactions;

    // Nếu mỗi stage chỉ dùng một Merkle root.
    private Integer stageMerkleTransactions;

    // Số stage đã thực sự được anchor.
    private Integer anchoredStages;

    // Tổng gas thực tế của các stage đã anchor.
    private BigInteger actualStageGasUsed;

    private Double transactionReductionPercent;

    private Boolean fullyAnchored;
}