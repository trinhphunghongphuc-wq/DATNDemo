package com.controller;

import com.entity.EvaluationResult;
import com.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.dto.evaluation.BatchStageEvaluationResponse;
import java.util.List;
import com.dto.evaluation.EvaluationCostResponse;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /*
     * Chạy cùng một bộ dữ liệu qua:
     * 1. HASH_PER_RECORD
     * 2. MERKLE_BATCHING
     */
    @PostMapping("/run")
    public List<EvaluationResult> runEvaluation(
            @RequestParam(defaultValue = "5")
            int recordCount,

            @RequestParam(defaultValue = "5")
            int repetitions
    ) {
        return evaluationService.runComparison(
                recordCount,
                repetitions
        );
    }

    @GetMapping
    public List<EvaluationResult> getAllResults() {
        return evaluationService.getAllResults();
    }

    @GetMapping("/{runId}")
    public List<EvaluationResult> getResultsByRunId(
            @PathVariable String runId
    ) {
        return evaluationService.getResultsByRunId(runId);
    }

    @GetMapping("/batches/{batchId}/stage-model")
    public BatchStageEvaluationResponse evaluateBatchStageModel(
            @PathVariable Long batchId
    ) {
        return evaluationService
                .evaluateBatchStageModel(batchId);
    }

    @GetMapping("/{runId}/cost")
    public List<EvaluationCostResponse> estimateRunCosts(
            @PathVariable String runId,

            @RequestParam
            BigDecimal l1GasPriceGwei,

            @RequestParam
            BigDecimal l2GasPriceGwei,

            @RequestParam
            BigDecimal ethPriceUsd
    ) {
        return evaluationService.estimateRunCosts(
                runId,
                l1GasPriceGwei,
                l2GasPriceGwei,
                ethPriceUsd
        );
    }
}