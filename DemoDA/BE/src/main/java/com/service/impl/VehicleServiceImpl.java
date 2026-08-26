package com.service.impl;


import com.dto.user.distributor.VehicleResponse;
import com.entity.Vehicle;
import com.repository.VehicleRepository;
import com.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public List<VehicleResponse> getMyVehicles(Long distributorId) {
        return vehicleRepository.findByDistributorIdAndActiveTrue(distributorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehiclePlate(vehicle.getVehiclePlate())
                .deviceId(vehicle.getDeviceId())
                .sensorFirmware(vehicle.getSensorFirmware())
                .distributorId(vehicle.getDistributorId())
                .active(vehicle.getActive())
                .build();
    }
}