package com.controller;


import com.dto.batch.UpdateExpiryDateRequest;
import com.dto.record.RecordRequest;
import com.dto.user.producer.ProducerBatchRequest;
import com.dto.user.producer.ProducerBatchResponse;
import com.dto.verify.VerifyAllResponse;
import com.service.ProducerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/producer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PRODUCER')")
public class ProducerController {

    private final ProducerService producerService;

    @PostMapping("/batches")
    public ProducerBatchResponse createBatch(
            @Valid @RequestBody ProducerBatchRequest request,
            Authentication authentication
    ) {
        return producerService.createBatch(request, authentication.getName());
    }

    @GetMapping("/batches")
    public List<ProducerBatchResponse> getMyBatches(Authentication authentication) {
        return producerService.getMyBatches(authentication.getName());
    }

    @GetMapping("/batches/{batchId}")
    public ProducerBatchResponse getMyBatchDetail(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        return producerService.getMyBatchDetail(batchId, authentication.getName());
    }

    @PostMapping("/batches/{batchId}/records")
    public ProducerBatchResponse addRecords(
            @PathVariable Long batchId,
            @Valid @RequestBody List<RecordRequest> requests,
            Authentication authentication
    ) {
        return producerService.addRecords(batchId, requests, authentication.getName());
    }

    @GetMapping("/batches/{batchId}/verify-all")
    public VerifyAllResponse verifyAll(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        return producerService.verifyAll(batchId, authentication.getName());
    }

    @PatchMapping("/batches/{batchId}/expiry-date")
    public ProducerBatchResponse updateExpiryDate(
            @PathVariable Long batchId,
            @Valid @RequestBody UpdateExpiryDateRequest request,
            Authentication authentication
    ) {
        return producerService.updateExpiryDate(batchId, request, authentication.getName());
    }
}