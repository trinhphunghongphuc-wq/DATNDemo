package com.service;



import com.dto.record.RecordItemResponse;
import com.dto.user.retailer.*;

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

    RetailUnitResponse updatePrice(
            Long retailUnitId,
            Long retailerId,
            UpdateRetailUnitPriceRequest request
    );

    List<RetailUnitPriceHistoryResponse> getPriceHistory(
            Long retailUnitId,
            Long retailerId
    );
}