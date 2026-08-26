package com.service;



import com.dto.user.distributor.VehicleResponse;

import java.util.List;

public interface VehicleService {

    List<VehicleResponse> getMyVehicles(Long distributorId);
}