package com.controller;

import com.entity.Record;
import com.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("@batchAccess.canRead(#p0, authentication)")
    public ResponseEntity<List<Record>> getRecordsByBatchId(
            @PathVariable Long batchId
    ) {
        return ResponseEntity.ok(
                recordService.getRecordsByBatchId(batchId)
        );
    }

    @GetMapping("/{recordKey}")
    @PreAuthorize("@batchAccess.canReadRecord(#p0, authentication)")
    public ResponseEntity<Record> getRecordByRecordKey(
            @PathVariable String recordKey
    ) {
        return ResponseEntity.ok(
                recordService.getByRecordKey(recordKey)
        );
    }
}