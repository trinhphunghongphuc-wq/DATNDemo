package com.service;



import com.dto.user.retailer.CreateRetailUnitRequest;
import com.dto.user.retailer.RetailUnitResponse;

import java.util.List;

public interface RetailUnitService {

    RetailUnitResponse createRetailUnit(Long batchId, Long retailerId, CreateRetailUnitRequest request);

    List<RetailUnitResponse> getMyRetailUnits(Long retailerId);

    List<RetailUnitResponse> getRetailUnitsByBatch(Long batchId, Long retailerId);

    RetailUnitResponse getRetailUnitDetail(Long retailUnitId, Long retailerId);
}