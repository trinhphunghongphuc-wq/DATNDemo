package com.service;


import com.dto.record.RecordRequest;
import com.entity.Batch;
import com.entity.Record;
import com.enums.Role;

import java.util.List;

public interface RecordService {


    List<Record> createRecordsForBatch(Batch batch, List<RecordRequest> requests, Role role);
    List<Record> getRecordsByBatchId(Long batchId);
    Record getByRecordKey(String recordKey);
    Record getByBatchIdAndRecordKey(Long batchId, String recordKey);


//    Record updateRecord(String recordKey, RecordRequest request, Role role);
//    void deleteRecord(String recordKey);
}