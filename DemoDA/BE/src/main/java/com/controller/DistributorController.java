package com.controller;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.distributor.TransportSensorRecordRequest;
import com.dto.user.distributor.VehicleResponse;
import com.dto.verify.VerifyAllResponse;
import com.security.CustomUserDetails;
import com.service.DistributorService;
import com.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distributor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DISTRIBUTOR')")
public class DistributorController {

    private final DistributorService distributorService;
    private final VehicleService vehicleService;

    @GetMapping("/batches")
    public ResponseEntity<List<AdminBatchListResponse>> getAssignedBatches(
            Authentication authentication
    ) {
        Long distributorId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributorService.getAssignedBatches(distributorId));
    }

    @PostMapping("/batches/{batchId}/receive")
    public ResponseEntity<AdminBatchListResponse> receiveBatch(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long distributorId = getCurrentUserId(authentication);

        System.out.println("===== RECEIVE BATCH DEBUG =====");
        System.out.println("batchId = " + batchId);
        System.out.println("current distributorId = " + distributorId);
        System.out.println("username = " + authentication.getName());
        System.out.println("principal class = " + authentication.getPrincipal().getClass().getName());

        return ResponseEntity.ok(distributorService.receiveBatch(batchId, distributorId));
    }

//    @PostMapping("/batches/{batchId}/transport-record")
//    public ResponseEntity<RecordItemResponse> addTransportRecord(
//            @PathVariable Long batchId,
//            @Valid @RequestBody RecordRequest request,
//            Authentication authentication
//    ) {
//        Long distributorId = getCurrentUserId(authentication);
//        return ResponseEntity.ok(distributorService.addTransportRecord(batchId, request, distributorId));
//    }

    @PostMapping("/batches/{batchId}/transport-record")
    public ResponseEntity<RecordItemResponse> addTransportRecord(
            @PathVariable Long batchId,
            @Valid @RequestBody TransportSensorRecordRequest request,

            Authentication authentication
    ) {
        Long distributorId = getCurrentUserId(authentication);
        return ResponseEntity.ok(
                distributorService.addTransportRecord(batchId, request, distributorId)
        );
    }

    @PostMapping("/batches/{batchId}/delivery-to-retailer")
    public ResponseEntity<AdminBatchListResponse> confirmDelivery(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long distributorId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributorService.DeliveryToDetailer(batchId, distributorId));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<BatchDetailResponse> getBatchDetail(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long distributorId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributorService.getBatchDetail(batchId, distributorId));
    }

    @GetMapping("/batches/{batchId}/verify-all")
    public ResponseEntity<VerifyAllResponse> verifyAllRecords(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        Long distributorId = getCurrentUserId(authentication);
        return ResponseEntity.ok(distributorService.verifyAllRecords(batchId, distributorId));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(Authentication authentication) {
        Long distributorId = getCurrentUserId(authentication);
        return ResponseEntity.ok(vehicleService.getMyVehicles(distributorId));
    }
    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }
}