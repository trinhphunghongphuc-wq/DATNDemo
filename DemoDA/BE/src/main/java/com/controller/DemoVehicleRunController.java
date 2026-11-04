package com.controller;

import com.dto.user.distributor.DemoRunRequest;
import com.dto.user.distributor.DemoRunResponse;
import com.security.CustomUserDetails;
import com.service.impl.DemoVehicleRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distributor/batches/{batchId}/demo-run")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DISTRIBUTOR')")
@ConditionalOnProperty(
        name = "app.demo-device.enabled",
        havingValue = "true"
)
public class DemoVehicleRunController {
    private final DemoVehicleRunService service;

    @PostMapping
    public DemoRunResponse start(
            @PathVariable Long batchId,
            @Valid @RequestBody DemoRunRequest request,
            Authentication authentication
    ) {
        return service.start(batchId, userId(authentication), request);
    }

    @GetMapping
    public DemoRunResponse status(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        return service.status(batchId, userId(authentication));
    }

    @PostMapping("/stop")
    public DemoRunResponse stop(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        return service.stop(batchId, userId(authentication));
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}