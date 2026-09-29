package com.service.impl;

import com.entity.Vehicle;
import com.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DemoVehicleKeyService {
    private final VehicleRepository vehicleRepository;

    @Transactional
    public long registerPublicKey(Long vehicleId, Long distributorId, String publicKeyBase64) {
        try {
            KeyFactory.getInstance("Ed25519").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Ed25519 demo public key", exception);
        }

        Vehicle vehicle = vehicleRepository.findActiveVehicleForUpdate(vehicleId, distributorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found or does not belong to this distributor"));
        vehicle.setDevicePublicKey(publicKeyBase64);
        vehicleRepository.save(vehicle);
        // Keep the counter to retain replay protection across demo sessions.
        return vehicle.getLastSequence() == null ? 0L : vehicle.getLastSequence();
    }
}
