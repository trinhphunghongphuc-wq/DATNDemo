package com.service.impl;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DemoVehicleRunRegistry {
    private final Map<Long, Long> busyVehicles = new HashMap<>();
    private final ThreadLocal<Long> demoVehicle = new ThreadLocal<>();

    public synchronized void reserve(Long vehicleId, Long batchId) {
        if (busyVehicles.containsKey(vehicleId)) {
            throw new IllegalStateException("Xe đang bận chạy demo cho lô khác");
        }
        busyVehicles.put(vehicleId, batchId);
    }

    public synchronized boolean isBusy(Long vehicleId) {
        return busyVehicles.containsKey(vehicleId);
    }

    public synchronized boolean isBusyForBatch(Long batchId) {
        return busyVehicles.containsValue(batchId);
    }

    public synchronized void release(Long vehicleId, Long batchId) {
        busyVehicles.remove(vehicleId, batchId);
    }

    public void runAsDemoVehicle(Long vehicleId, Runnable action) {
        demoVehicle.set(vehicleId);
        try {
            action.run();
        } finally {
            demoVehicle.remove();
        }
    }

    public void checkTransportAllowed(Long vehicleId) {
        if (isBusy(vehicleId) && !vehicleId.equals(demoVehicle.get())) {
            throw new IllegalStateException(
                    "Xe đang bận chạy demo; không thể gửi điểm vận chuyển thủ công"
            );
        }
    }
}