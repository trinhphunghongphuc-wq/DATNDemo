package com.controller;

import com.security.CustomUserDetails;
import com.service.impl.DemoSignedRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/distributor/batches/{batchId}/demo-signed-record")
@PreAuthorize("hasRole('DISTRIBUTOR')")
@ConditionalOnProperty(name = "app.demo-device.enabled", havingValue = "true")
public class DemoSignedRecordController {
    private final DemoSignedRecordService service;

    public DemoSignedRecordController(DemoSignedRecordService service) {
        this.service = service;
    }

    public record PrepareRequest(
            @NotNull Long vehicleId,
            @NotBlank String gps,
            @NotNull Double temperature,
            @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double humidity
    ) {}

    @PostMapping
    public Map<String, Object> prepare(
            @PathVariable Long batchId,
            @Valid @RequestBody PrepareRequest request,
            Authentication authentication
    ) {
        Long distributorId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return service.prepare(batchId, distributorId, request);
    }
}
