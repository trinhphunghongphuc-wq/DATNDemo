package com.service;



import com.dto.record.RecordItemResponse;
import com.dto.user.retailer.CreateRetailUnitRequest;
import com.dto.user.retailer.RetailStorageReadingRequest;
import com.dto.user.retailer.RetailUnitResponse;

import java.util.List;

public interface RetailUnitService {

    RetailUnitResponse createRetailUnit(Long batchId, Long retailerId, CreateRetailUnitRequest request);

    List<RetailUnitResponse> getMyRetailUnits(Long retailerId);

    List<RetailUnitResponse> getRetailUnitsByBatch(Long batchId, Long retailerId);

    RetailUnitResponse getRetailUnitDetail(Long retailUnitId, Long retailerId);


    RecordItemResponse addStorageReading(
            Long retailUnitId,
            Long retailerId,
            RetailStorageReadingRequest request
    );
}