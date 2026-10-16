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

    // Chỉ các dòng trung bình: giữ nguyên dữ liệu cho trang Admin hiện tại.
    List<EvaluationResult>
    findByRunIdAndSampleIndexIsNullOrderByEvaluationModelAsc(String runId);

    List<EvaluationResult>
    findAllBySampleIndexIsNullOrderByCreatedAtDesc();

    // Các lần đo riêng để xuất bảng thực nghiệm.
    List<EvaluationResult>
    findByRunIdAndSampleIndexIsNotNullOrderBySampleIndexAscEvaluationModelAsc(
            String runId
    );
}