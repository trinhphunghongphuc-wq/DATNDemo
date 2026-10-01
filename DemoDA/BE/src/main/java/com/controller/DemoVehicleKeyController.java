package com.controller;

import com.service.impl.DemoVehicleKeyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.security.CustomUserDetails;

import java.util.Map;

/** Enabled only for local demonstration. The browser never sends its private key. */
@RestController
@RequestMapping("/api/distributor/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DISTRIBUTOR')")
@ConditionalOnProperty(name = "app.demo-device.enabled", havingValue = "true")
public class DemoVehicleKeyController {
    private final DemoVehicleKeyService demoVehicleKeyService;

    public record DemoKeyRequest(@NotBlank String publicKey) {}

    @PostMapping("/{vehicleId}/demo-public-key")
    public Map<String, Long> registerDemoKey(
            @PathVariable Long vehicleId,
            @Valid @RequestBody DemoKeyRequest request,
            Authentication authentication) {
        Long distributorId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        long lastSequence = demoVehicleKeyService.registerPublicKey(
                vehicleId, distributorId, request.publicKey());
        return Map.of("vehicleId", vehicleId, "lastSequence", lastSequence);
    }
}
