package com.service;


import com.dto.batch.UpdateExpiryDateRequest;
import com.dto.user.producer.ProducerBatchRequest;

import com.dto.record.RecordRequest;
import com.dto.user.producer.ProducerBatchResponse;
import com.dto.verify.VerifyAllResponse;

import java.util.List;

public interface ProducerService {

    ProducerBatchResponse createBatch(ProducerBatchRequest request, String username);

    List<ProducerBatchResponse> getMyBatches(String username);

    ProducerBatchResponse getMyBatchDetail(Long batchId, String username);

    ProducerBatchResponse addRecords(Long batchId, List<RecordRequest> requests, String username);

    ProducerBatchResponse updateExpiryDate(Long batchId, UpdateExpiryDateRequest request, String username);

    VerifyAllResponse verifyAll(Long batchId, String username);
}