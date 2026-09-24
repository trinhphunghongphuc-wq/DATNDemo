package com.dto.blockchain;

import com.enums.RecordStage;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Getter
@Builder
public class StageAnchorResponse {

    private Long id;

    private Long batchId;

    private RecordStage recordStage;

    private String transactionHash;

    private BigInteger blockNumber;

    private BigInteger gasUsed;

    private LocalDateTime anchoredAt;
}