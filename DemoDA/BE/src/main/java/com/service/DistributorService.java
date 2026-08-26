package com.service;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.RecordItemResponse;
import com.dto.record.RecordRequest;
import com.dto.user.distributor.TransportSensorRecordRequest;
import com.dto.verify.VerifyAllResponse;

import java.util.List;

public interface DistributorService {

    List<AdminBatchListResponse> getAssignedBatches(Long distributorId);

    AdminBatchListResponse receiveBatch(Long batchId, Long distributorId);

    RecordItemResponse addTransportRecord(
            Long batchId,
            TransportSensorRecordRequest request,
            Long distributorId
    );

    AdminBatchListResponse DeliveryToDetailer(Long batchId, Long distributorId);

    BatchDetailResponse getBatchDetail(Long batchId, Long distributorId);

    VerifyAllResponse verifyAllRecords(Long batchId, Long distributorId);
}