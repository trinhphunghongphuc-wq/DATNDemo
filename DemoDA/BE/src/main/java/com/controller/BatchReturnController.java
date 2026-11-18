package com.controller;

import com.dto.returns.ReturnCaseDtos.*;
import com.security.CustomUserDetails;
import com.service.impl.BatchReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/return-cases")
@PreAuthorize("hasAnyRole('PRODUCER', 'DISTRIBUTOR', 'RETAILER')")
@RequiredArgsConstructor
public class BatchReturnController {
    private final BatchReturnService returns;

    @GetMapping
    public Page<Summary> list(@AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam(defaultValue = "0") int page) {
        return returns.list(user, page);
    }

    @GetMapping("/{id}")
    public Detail get(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return returns.get(id, user);
    }

    @GetMapping("/{id}/telemetry")
    public Page<Event> telemetry(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam(defaultValue = "0") int page) {
        return returns.telemetry(id, user, page);
    }

    @PostMapping("/{id}/approve")
    public Detail approve(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                          @Valid @RequestBody Approval body) {
        return returns.approve(id, user, body);
    }

    @PostMapping("/{id}/trip")
    public Detail trip(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                       @Valid @RequestBody Trip body) {
        return returns.startTrip(id, user, body);
    }

    @PostMapping("/{id}/telemetry")
    public Event addReading(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                            @Valid @RequestBody SignedReading body) {
        return returns.addReading(id, user, body);
    }

    @PostMapping("/{id}/receive")
    public Detail receive(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                          @Valid @RequestBody Receipt body) {
        return returns.receive(id, user, body);
    }

    @PostMapping("/{id}/inspect")
    public Detail inspect(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                          @Valid @RequestBody Inspection body) {
        return returns.inspect(id, user, body);
    }

    @PostMapping("/{id}/decision")
    public Detail decide(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                         @Valid @RequestBody Decision body) {
        return returns.decide(id, user, body);
    }

    @PostMapping("/{id}/execute")
    public Detail execute(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                          @Valid @RequestBody Execution body) {
        return returns.execute(id, user, body);
    }

    @PostMapping("/{id}/close")
    public Detail close(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return returns.close(id, user);
    }

    @PostMapping("/{id}/evidence")
    public Map<String, String> evidence(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                                        @RequestParam MultipartFile file) {
        return Map.of("cid", returns.uploadEvidence(id, user, file));
    }
}
