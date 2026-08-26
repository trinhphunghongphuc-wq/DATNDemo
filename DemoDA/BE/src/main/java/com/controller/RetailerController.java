package com.controller;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.producer.RejectDeliveryRequest;
import com.dto.user.retailer.CreateRetailUnitRequest;
import com.dto.user.retailer.RetailUnitResponse;
import com.dto.verify.VerifyAllResponse;
import com.security.CustomUserDetails;
import com.service.RetailerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.service.RetailUnitService;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/retailer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RETAILER')")
public class RetailerController {

    private final RetailerService retailerService;
    private final RetailUnitService retailUnitService;


    @GetMapping("/batches")
    public ResponseEntity<List<AdminBatchListResponse>> getReceivedBatches(Authentication authentication) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.getReceivedBatches(retailerId));
    }

//    @PostMapping("/batches/{batchId}/receive")
//    public ResponseEntity<AdminBatchListResponse> receiveBatch(
//            @PathVariable Long batchId,
//            Authentication authentication
//    ) {
//        Long retailerId = getCurrentUserId(authentication);
//        return ResponseEntity.ok(retailerService.receiveBatch(batchId, retailerId));
//    }

    @PostMapping("/batches/{batchId}/retail-record")
    public ResponseEntity<RecordItemResponse> addRetailRecord(
            @PathVariable Long batchId,
            @Valid @RequestBody RecordRequest request,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.addRetailRecord(batchId, request, retailerId));
    }

    @PostMapping("/batches/{batchId}/confirm-delivery")
    public ResponseEntity<AdminBatchListResponse> confirmDelivery( @PathVariable Long batchId, Authentication authentication )
    {   Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok( retailerService.confirmDelivery(batchId, retailerId) );
    }

    @GetMapping("/batches/{batchId}/expiry-status")
    public ResponseEntity<Map<String, Object>> getExpiryStatus(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.getExpiryStatus(batchId, retailerId));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<BatchDetailResponse> getBatchDetail(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.getBatchDetail(batchId, retailerId));
    }

    @GetMapping("/batches/{batchId}/verify-all")
    public ResponseEntity<VerifyAllResponse> verifyAllRecords(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.verifyAllRecords(batchId, retailerId));
    }

    @GetMapping("/batches/{batchId}/qr")
    public ResponseEntity<Map<String, Object>> getVerificationQr(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.getVerificationQr(batchId, retailerId));
    }


    @PostMapping("/batches/{batchId}/retail-units")
    public ResponseEntity<RetailUnitResponse> createRetailUnit(
            @PathVariable Long batchId,
            @Valid @RequestBody CreateRetailUnitRequest request,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailUnitService.createRetailUnit(batchId, retailerId, request));
    }

    @GetMapping("/retail-units")
    public ResponseEntity<List<RetailUnitResponse>> getMyRetailUnits(Authentication authentication) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailUnitService.getMyRetailUnits(retailerId));
    }

    @GetMapping("/batches/{batchId}/retail-units")
    public ResponseEntity<List<RetailUnitResponse>> getRetailUnitsByBatch(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailUnitService.getRetailUnitsByBatch(batchId, retailerId));
    }

    @GetMapping("/retail-units/{retailUnitId}")
    public ResponseEntity<RetailUnitResponse> getRetailUnitDetail(
            @PathVariable Long retailUnitId,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailUnitService.getRetailUnitDetail(retailUnitId, retailerId));
    }

    @PostMapping("/batches/{batchId}/reject-delivery")
    public ResponseEntity<AdminBatchListResponse> rejectDelivery(
            @PathVariable Long batchId,
            @RequestBody RejectDeliveryRequest request,
            Authentication authentication
    ) {
        Long retailerId = getCurrentUserId(authentication);
        return ResponseEntity.ok(retailerService.rejectDelivery(batchId, retailerId, request));
    }

    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }
}