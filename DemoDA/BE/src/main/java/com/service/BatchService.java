package com.service;

import com.dto.batch.*;
import com.entity.Batch;
import com.enums.Role;

import java.util.List;

public interface BatchService {


    BatchResponse createBatch(BatchRequest request, Role role);
    Batch createBatchEntity(BatchRequest request, Role role);

    List<BatchListResponse> getAllBatches();
//    AdminBatchListResponse anchorBatchRoot(Long batchId) throws Exception;

    Batch getBatchById(Long id);
    ProofResponse getProof(Long batchId, String recordKey);
    boolean verifyRecordProof(Long batchId, String recordKey);

    //BatchDetail
    BatchDetailResponse getBatchDetail(Long id);
    BatchDetailResponse getBatchByCode(String batchCode);
}