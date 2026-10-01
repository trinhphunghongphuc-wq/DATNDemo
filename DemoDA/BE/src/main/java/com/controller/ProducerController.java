package com.controller;


import com.dto.batch.UpdateExpiryDateRequest;
import com.dto.record.RecordRequest;
import com.dto.user.producer.AddPartnerRequest;
import com.dto.user.producer.ProducerBatchRequest;
import com.dto.user.producer.ProducerBatchResponse;
import com.dto.verify.VerifyAllResponse;
import com.service.ProducerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.dto.user.producer.CompanyProfileResponse;
import com.enums.Role;
import com.service.impl.ProducerPartnerService;

import java.util.List;

@RestController
@RequestMapping("/api/producer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PRODUCER')")
public class ProducerController {

    private final ProducerService producerService;
    private final ProducerPartnerService partnerService;

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

    @GetMapping("/partners/directory")
    public List<CompanyProfileResponse> getPartnerDirectory(
            @RequestParam Role role
    ) {
        return partnerService.directory(role);
    }

    @GetMapping("/partners")
    public List<CompanyProfileResponse> getMyPartners(
            @RequestParam Role role,
            Authentication authentication
    ) {
        return partnerService.myPartners(
                authentication.getName(),
                role
        );
    }

    @PostMapping("/partners")
    public CompanyProfileResponse addPartner(
            @RequestBody AddPartnerRequest request,
            Authentication authentication
    ) {
        if (request == null || request.partnerId() == null) {
            throw new IllegalArgumentException("partnerId is required");
        }

        return partnerService.addPartner(
                authentication.getName(),
                request.partnerId()
        );
    }

    @DeleteMapping("/partners/{partnerId}")
    public void removePartner(
            @PathVariable Long partnerId,
            Authentication authentication
    ) {
        partnerService.removePartner(
                authentication.getName(),
                partnerId
        );
    }
}