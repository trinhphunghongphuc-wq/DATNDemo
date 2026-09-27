package com.controller;

import com.dto.user.distributor.TransportJourneyResponse;
import com.dto.user.distributor.TransportRouteRequest;
import com.security.CustomUserDetails;
import com.service.impl.TransportJourneyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transport/batches")
@RequiredArgsConstructor
public class TransportJourneyController {

    private final TransportJourneyService journeys;

    @PostMapping("/{batchId}/route")
    @PreAuthorize("hasRole('DISTRIBUTOR')")
    public TransportJourneyResponse plan(
            @PathVariable Long batchId,
            @Valid @RequestBody TransportRouteRequest request,
            Authentication authentication
    ) {
        return journeys.plan(
                batchId,
                request,
                (CustomUserDetails) authentication.getPrincipal()
        );
    }

    @GetMapping("/{batchId}/journey")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISTRIBUTOR', 'RETAILER')")
    public TransportJourneyResponse get(
            @PathVariable Long batchId,
            Authentication authentication
    ) {
        return journeys.get(
                batchId,
                (CustomUserDetails) authentication.getPrincipal()
        );
    }
}