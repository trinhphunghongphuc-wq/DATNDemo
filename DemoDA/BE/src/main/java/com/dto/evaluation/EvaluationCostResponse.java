package com.dto.evaluation;

import com.enums.EvaluationModel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Builder
public class EvaluationCostResponse {

    private String runId;
    private EvaluationModel evaluationModel;

    private Integer recordCount;
    private Integer transactionCount;
    private BigInteger totalGasUsed;

    private BigDecimal l1GasPriceGwei;
    private BigDecimal l1CostEth;
    private BigDecimal l1CostUsd;

    private BigDecimal l2GasPriceGwei;
    private BigDecimal l2ExecutionCostEth;
    private BigDecimal l2ExecutionCostUsd;

    private BigDecimal ethPriceUsd;

    private String note;
}