package com.controller;


import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.service.VerifyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dto.verify.MerkleProofResponse;

@RestController
@RequestMapping("/api/verify")
public class VerifyController {

    private final VerifyService verifyService;

    public VerifyController(VerifyService verifyService) {
        this.verifyService = verifyService;
    }

    @PostMapping
    public ResponseEntity<VerifyResponse> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(verifyService.verify(request));
    }


    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Merkle proof chỉ được sinh khi client gọi endpoint này.
     * Proof không được lưu cố định trong PostgreSQL hoặc IPFS.
     */
    @GetMapping("/batches/{batchId}/records/{recordKey}/proof")
    public MerkleProofResponse generateMerkleProof(
            @PathVariable Long batchId,
            @PathVariable String recordKey
    ) {
        return verifyService.generateMerkleProof(
                batchId,
                recordKey
        );
    }
}