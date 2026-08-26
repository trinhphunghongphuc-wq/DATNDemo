package com.controller;

import com.service.impl.BlockchainConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainConnectionService blockchainConnectionService;


    @GetMapping("/rpc")
    public String testRpc() throws Exception {
        return blockchainConnectionService.getClientVersion();
    }

    @GetMapping("/block")
    public Long testBlock() throws Exception {
        return blockchainConnectionService.getBlockNumber();
    }


    @PostMapping("/set-root")
    public String setRoot(
            @RequestParam Long batchId,
            @RequestParam String merkleRoot
    ) throws Exception {

        return blockchainConnectionService.setRoot(batchId, merkleRoot);
    }

    //get merkleRoot theo batchID
    @GetMapping("/get-root/{batchId}")
    public String getRoot(@PathVariable Long batchId) throws Exception {
        return blockchainConnectionService.getRoot(batchId);
    }
}