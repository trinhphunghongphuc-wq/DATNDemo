package com.controller;

import com.enums.RecordStage;
import com.service.impl.BlockchainConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainConnectionService
            blockchainConnectionService;

    /*
     * Kiểm tra Spring Boot có kết nối được Ethereum RPC hay không.
     */
    @GetMapping("/rpc")
    public String testRpc() throws Exception {
        return blockchainConnectionService
                .getClientVersion();
    }

    /*
     * Kiểm tra block hiện tại của blockchain.
     */
    @GetMapping("/block")
    public Long testBlock() throws Exception {
        return blockchainConnectionService
                .getBlockNumber();
    }

    /*
     * Lấy địa chỉ ví mà backend đang sử dụng để ký transaction.
     */
    @GetMapping("/wallet")
    public String getWalletAddress() {
        return blockchainConnectionService
                .getWalletAddress();
    }

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Đọc Merkle root on-chain theo từng giai đoạn.
     *
     * Endpoint ghi root trực tiếp đã được loại bỏ.
     * Việc anchor phải đi qua AdminService để kiểm tra batch,
     * stage root và cập nhật trạng thái trong PostgreSQL.
     */
    @GetMapping(
            "/batches/{batchId}/stages/{stage}/root"
    )
    public String getStageRoot(
            @PathVariable Long batchId,
            @PathVariable RecordStage stage
    ) throws Exception {
        return blockchainConnectionService
                .getStageRoot(
                        batchId,
                        stage
                );
    }
}