package com.controller;

import com.service.SmartContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/contract")
@RequiredArgsConstructor
public class SmartContractController {

    private final SmartContractService smartContractService;

    @GetMapping("/root/{batchId}")
    public String getRoot(@PathVariable Long batchId) throws Exception {
        return smartContractService.getRoot(batchId);
    }

    @PostMapping("/root/{batchId}")
    public String setRoot(
            @PathVariable Long batchId,
            @RequestParam String merkleRoot
    ) throws Exception {
        return smartContractService.setRoot(batchId, merkleRoot);
    }
}