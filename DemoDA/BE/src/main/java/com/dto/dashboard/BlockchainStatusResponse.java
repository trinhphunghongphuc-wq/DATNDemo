package com.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainStatusResponse {

    private boolean connected;
    private String clientVersion;
    private Long currentBlock;
    private String contractAddress;
    private String walletAddress;
}