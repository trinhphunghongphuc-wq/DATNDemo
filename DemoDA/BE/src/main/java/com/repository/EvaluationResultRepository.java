package com.repository;

import com.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationResultRepository
        extends JpaRepository<EvaluationResult, Long> {

    List<EvaluationResult> findByRunIdOrderByEvaluationModelAsc(
            String runId
    );

    List<EvaluationResult> findAllByOrderByCreatedAtDesc();
}