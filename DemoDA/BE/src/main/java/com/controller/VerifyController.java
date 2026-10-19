package com.controller;

import com.dto.verify.MerkleProofResponse;
import com.dto.verify.OnChainVerificationResponse;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.repository.BatchRepository;
import com.security.CustomUserDetails;
import com.service.VerifyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verify")
@PreAuthorize("hasRole('RETAILER')")
public class VerifyController {

    private final VerifyService verifyService;
    private final BatchRepository batchRepository;

    public VerifyController(
            VerifyService verifyService,
            BatchRepository batchRepository
    ) {
        this.verifyService = verifyService;
        this.batchRepository = batchRepository;
    }

    @PostMapping
    public ResponseEntity<VerifyResponse> verify(
            @Valid @RequestBody VerifyRequest request,
            Authentication authentication
    ) {
        requireOwnedBatch(request.getBatchId(), authentication);
        return ResponseEntity.ok(verifyService.verify(request));
    }

    /*
     * Merkle proof chỉ được sinh khi client gọi endpoint này.
     * Proof không được lưu cố định trong PostgreSQL hoặc IPFS.
     */
    @GetMapping("/batches/{batchId}/records/{recordKey}/proof")
    public MerkleProofResponse generateMerkleProof(
            @PathVariable Long batchId,
            @PathVariable String recordKey,
            Authentication authentication
    ) {
        requireOwnedBatch(batchId, authentication);
        return verifyService.generateMerkleProof(batchId, recordKey);
    }

    /*
     * Xác minh dữ liệu off-chain bằng Merkle proof
     * và stage root bất biến trên blockchain.
     */
    @GetMapping("/batches/{batchId}/records/{recordKey}/verify-on-chain")
    public OnChainVerificationResponse verifyOnChain(
            @PathVariable Long batchId,
            @PathVariable String recordKey,
            Authentication authentication
    ) {
        requireOwnedBatch(batchId, authentication);
        return verifyService.verifyRecordOnChain(batchId, recordKey);
    }

    private void requireOwnedBatch(Long batchId, Authentication authentication) {
        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        if (batchId == null ||
                batchRepository
                        .findByIdAndRetailerId(batchId, user.getId())
                        .isEmpty()) {
            throw new AccessDeniedException(
                    "Bạn không có quyền xác minh lô hàng này."
            );
        }
    }
}