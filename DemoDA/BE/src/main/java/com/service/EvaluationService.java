package com.service;

import com.entity.EvaluationResult;
import com.dto.evaluation.EvaluationCostResponse;
import java.math.BigDecimal;
import java.util.List;
import com.dto.evaluation.BatchStageEvaluationResponse;

public interface EvaluationService {

    List<EvaluationResult> runComparison(
            int recordCount,
            int repetitions
    );


    List<EvaluationResult> getResultsByRunId(String runId);

    List<EvaluationResult> getAllResults();

    BatchStageEvaluationResponse evaluateBatchStageModel(
            Long batchId
    );

    List<EvaluationCostResponse> estimateRunCosts(
            String runId,
            BigDecimal l1GasPriceGwei,
            BigDecimal l2GasPriceGwei,
            BigDecimal ethPriceUsd
    );
}