package com.controller;

import com.dto.blockchain.WalletAnchorDto.*;
import com.security.CustomUserDetails;
import com.service.impl.WalletAnchorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet-anchors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PRODUCER','DISTRIBUTOR','RETAILER')")
public class WalletAnchorController {
    private final WalletAnchorService walletAnchors;

    @GetMapping("/wallet")
    public Map<String, String> wallet(@AuthenticationPrincipal CustomUserDetails user) {
        return Collections.singletonMap("walletAddress", walletAnchors.wallet(user));
    }

    @PutMapping("/wallet")
    public Map<String, String> updateWallet(@AuthenticationPrincipal CustomUserDetails user,
                                           @Valid @RequestBody Address request) {
        return Map.of("walletAddress", walletAnchors.updateWallet(user, request.walletAddress()));
    }

    @GetMapping("/batches/{id}/signing-request")
    public SigningRequest signingRequest(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user)
            throws Exception {
        return walletAnchors.signingRequest(id, user);
    }

    @PostMapping("/batches/{id}/approval")
    public ResponseEntity<Void> approve(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user,
                                        @Valid @RequestBody SignedApproval request) throws Exception {
        walletAnchors.approve(id, user, request);
        return ResponseEntity.noContent().build();
    }
}
