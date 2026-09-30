package com.service.impl;


import com.dto.user.distributor.VehicleCreateRequest;
import com.dto.user.distributor.VehicleResponse;
import com.entity.Vehicle;
import com.repository.VehicleRepository;
import com.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public VehicleResponse createVehicle(
            VehicleCreateRequest request,
            Long distributorId
    ) {
        String plate = request.getVehiclePlate().trim();
        String deviceId = "IOT-DEMO-" + java.util.UUID.randomUUID();
        String firmware = request.getSensorFirmware().trim();

        if (vehicleRepository.existsByVehiclePlateIgnoreCase(plate)) {
            throw new IllegalArgumentException("Biển số xe đã tồn tại");
        }
        if (vehicleRepository.existsByDeviceIdIgnoreCase(deviceId)) {
            throw new IllegalArgumentException("Mã thiết bị đã tồn tại");
        }

        Vehicle vehicle = Vehicle.builder()
                .vehiclePlate(plate)
                .deviceId(deviceId)
                .sensorFirmware(firmware)
                .distributorId(distributorId)
                .active(true)
                .lastSequence(0L)
                .build();

        return toResponse(vehicleRepository.save(vehicle));
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehiclePlate(vehicle.getVehiclePlate())
                .deviceId(vehicle.getDeviceId())
                .sensorFirmware(vehicle.getSensorFirmware())
                .distributorId(vehicle.getDistributorId())
                .active(vehicle.getActive())
                .lastSequence(vehicle.getLastSequence())
                .build();
    }
}
