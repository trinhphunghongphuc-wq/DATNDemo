package com.controller;

import com.dto.batch.*;
import com.dto.record.RecordRequest;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.entity.Batch;
import com.entity.Record;
import com.enums.Role;
import com.repository.RecordRepository;
import com.service.BatchService;
import com.service.RecordService;
import com.service.VerifyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;
    private final RecordService recordService;
    private final VerifyService verifyService;
    private final RecordRepository recordRepository;

    public BatchController(BatchService batchService, RecordService recordService, VerifyService verifyService, RecordRepository recordRepository) {
        this.batchService = batchService;
        this.recordService = recordService;
        this.verifyService = verifyService;
        this.recordRepository = recordRepository;
    }

    @PostMapping
    public ResponseEntity<BatchResponse> createBatch(
            @Valid @RequestBody BatchRequest request,
            Authentication authentication
    ) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        Role role = Role.valueOf(authority.replace("ROLE_", ""));

        System.out.println("AUTHORITIES = " + authentication.getAuthorities());
        System.out.println("ROLE PARSED = " + role);

        return ResponseEntity.ok(batchService.createBatch(request, role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchDetailResponse> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.getBatchDetail(id));
    }

    @GetMapping("/{batchId}/proof/{recordKey}")
    public ResponseEntity<ProofResponse> getProof(@PathVariable Long batchId,
                                                  @PathVariable String recordKey) {
        return ResponseEntity.ok(batchService.getProof(batchId, recordKey));
    }

    @GetMapping("/{batchId}/verify/{recordKey}")
    public ResponseEntity<VerifyResponse> verifyRecord(
            @PathVariable Long batchId,
            @PathVariable String recordKey
    ) {

        VerifyRequest request = new VerifyRequest();
        request.setBatchId(batchId);


        Record record = recordRepository
                .findByBatchIdAndRecordKey(batchId, recordKey)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        request.setRawJson(record.getRawJson());

        return ResponseEntity.ok(verifyService.verify(request));
    }

    @PostMapping("/{batchId}/records")
    public ResponseEntity<List<Record>> addRecordsToBatch(
            @PathVariable Long batchId,
            @RequestBody List<RecordRequest> requests,
            Authentication authentication
    ) {
        Batch batch = batchService.getBatchById(batchId);

        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        Role role = Role.valueOf(authority.replace("ROLE_", ""));

        List<Record> records = recordService.createRecordsForBatch(batch, requests, role);
        return ResponseEntity.ok(records);
    }

    @GetMapping
    public ResponseEntity<List<BatchListResponse>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }
}