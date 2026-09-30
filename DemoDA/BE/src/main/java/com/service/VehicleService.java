package com.service;



import com.dto.user.distributor.VehicleResponse;
import com.dto.user.distributor.VehicleCreateRequest;
import java.util.List;

public interface VehicleService {

    List<VehicleResponse> getMyVehicles(Long distributorId);
    VehicleResponse createVehicle(VehicleCreateRequest request, Long distributorId);
}