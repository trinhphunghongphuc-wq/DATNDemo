package com.service.impl;

import com.blockchain.contract.TraceabilityRegistry;
import com.enums.RecordStage;
import com.util.HexUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockchainConnectionService {

    private final Web3j web3j;
    private final Credentials credentials;

    @Getter
    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Value("${blockchain.gas.price}")
    private BigInteger gasPrice;

    @Value("${blockchain.gas.limit}")
    private BigInteger gasLimit;

    private TraceabilityRegistry loadContract() {
        return TraceabilityRegistry.load(
                contractAddress,
                web3j,
                credentials,
                new StaticGasProvider(
                        gasPrice,
                        gasLimit
                )
        );
    }

    public String getClientVersion() throws Exception {
        return web3j.web3ClientVersion()
                .send()
                .getWeb3ClientVersion();
    }

    public Long getBlockNumber() throws Exception {
        return web3j.ethBlockNumber()
                .send()
                .getBlockNumber()
                .longValue();
    }

    public String getWalletAddress() {
        return credentials.getAddress();
    }

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Không ghi một Merkle root chung cho toàn bộ batch.
     *
     * Mỗi giai đoạn Producer, Distributor và Retailer
     * được anchor bằng một transaction riêng.
     *
     * Trả về TransactionReceipt để phía service có thể lưu:
     * - transaction hash;
     * - block number;
     * - gas used;
     * - trạng thái giao dịch.
     */
    public TransactionReceipt anchorStageRoot(
            Long batchId,
            RecordStage stage,
            String merkleRootHex
    ) throws Exception {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid batchId"
            );
        }

        if (stage == null) {
            throw new IllegalArgumentException(
                    "Record stage is required"
            );
        }

        byte[] rootBytes = toBytes32(
                merkleRootHex,
                "Merkle root"
        );

        TraceabilityRegistry contract =
                loadContract();

        return contract.anchorStageRoot(
                BigInteger.valueOf(batchId),
                toBlockchainStage(stage),
                rootBytes
        ).send();
    }

    /*
     * Đọc root on-chain của đúng giai đoạn.
     */
    public String getStageRoot(
            Long batchId,
            RecordStage stage
    ) throws Exception {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid batchId"
            );
        }

        if (stage == null) {
            throw new IllegalArgumentException(
                    "Record stage is required"
            );
        }

        byte[] rootBytes = loadContract()
                .getStageRoot(
                        BigInteger.valueOf(batchId),
                        toBlockchainStage(stage)
                )
                .send();

        return HexUtil.bytesToHex(rootBytes);
    }

    /*
     * Xác minh Merkle proof trực tiếp bằng smart contract.
     *
     * Proof được sinh khi có yêu cầu và truyền vào contract,
     * không được lưu trong PostgreSQL, IPFS hoặc blockchain.
     */
    public boolean verifyStageRecord(
            Long batchId,
            RecordStage stage,
            String leafHashHex,
            List<String> proofHex,
            Integer stageLeafIndex
    ) throws Exception {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid batchId"
            );
        }

        if (stage == null) {
            throw new IllegalArgumentException(
                    "Record stage is required"
            );
        }

        if (stageLeafIndex == null
                || stageLeafIndex < 0) {
            throw new IllegalArgumentException(
                    "Invalid stage leaf index"
            );
        }

        if (proofHex == null) {
            throw new IllegalArgumentException(
                    "Merkle proof is required"
            );
        }

        byte[] leafBytes = toBytes32(
                leafHashHex,
                "Leaf hash"
        );

        List<byte[]> proofBytes = proofHex.stream()
                .map(hash ->
                        toBytes32(
                                hash,
                                "Proof hash"
                        )
                )
                .toList();

        return loadContract()
                .verifyStageRecord(
                        BigInteger.valueOf(batchId),
                        toBlockchainStage(stage),
                        leafBytes,
                        proofBytes,
                        BigInteger.valueOf(stageLeafIndex)
                )
                .send();
    }

    /*
     * Solidity enum:
     * 0 = PRODUCER
     * 1 = DISTRIBUTOR
     * 2 = RETAILER
     */
    private BigInteger toBlockchainStage(
            RecordStage stage
    ) {
        return switch (stage) {
            case PRODUCER -> BigInteger.ZERO;
            case DISTRIBUTOR -> BigInteger.ONE;
            case RETAILER -> BigInteger.TWO;
        };
    }

    /*
     * bytes32 yêu cầu đúng 32 byte,
     * tương ứng chuỗi hash hexadecimal dài 64 ký tự.
     */
    private byte[] toBytes32(
            String hexadecimalValue,
            String fieldName
    ) {
        if (hexadecimalValue == null
                || hexadecimalValue.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        byte[] bytes =
                HexUtil.hexToBytes(hexadecimalValue);

        if (bytes.length != 32) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must contain exactly 32 bytes"
            );
        }

        return bytes;
    }
}