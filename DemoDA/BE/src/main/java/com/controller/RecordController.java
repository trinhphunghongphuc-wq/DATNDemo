package com.controller;

import com.dto.record.RecordRequest;
import com.entity.Batch;
import com.service.BatchService;
import com.service.RecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.entity.Record;
import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;
    private final BatchService batchService;
    public RecordController(RecordService recordService, BatchService batchService) {
        this.recordService = recordService;
        this.batchService = batchService;
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<Record>> getRecordsByBatchId(@PathVariable Long batchId) {
        List<Record> records = recordService.getRecordsByBatchId(batchId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{recordKey}")
    public ResponseEntity<Record> getRecordByRecordKey(@PathVariable String recordKey) {
        Record record = recordService.getByRecordKey(recordKey);
        return ResponseEntity.ok(record);
    }


}