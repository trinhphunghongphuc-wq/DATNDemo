package com.service;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.producer.RejectDeliveryRequest;
import com.dto.verify.VerifyAllResponse;

import java.util.List;
import java.util.Map;

public interface RetailerService {

    List<AdminBatchListResponse> getReceivedBatches(Long retailerId);

    AdminBatchListResponse receiveBatch(Long batchId, Long retailerId);

    RecordItemResponse addRetailRecord(Long batchId, RecordRequest request, Long retailerId);

    Map<String, Object> getExpiryStatus(Long batchId, Long retailerId);

    BatchDetailResponse getBatchDetail(Long batchId, Long retailerId);

    VerifyAllResponse verifyAllRecords(Long batchId, Long retailerId);

    Map<String, Object> getVerificationQr(Long batchId, Long retailerId);

    AdminBatchListResponse confirmDelivery(Long batchId, Long retailerId);

    AdminBatchListResponse rejectDelivery(Long batchId, Long retailerId, RejectDeliveryRequest request);
}