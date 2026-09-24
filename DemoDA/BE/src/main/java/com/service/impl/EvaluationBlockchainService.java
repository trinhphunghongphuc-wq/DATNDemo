package com.service.impl;

import com.blockchain.contract.CertificationBenchmark;
import com.util.HexUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@Service
public class EvaluationBlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final BigInteger gasPrice;
    private final BigInteger gasLimit;

    public EvaluationBlockchainService(
            Web3j web3j,
            Credentials credentials,
            @Value("${blockchain.benchmark.contract.address}")
            String contractAddress,
            @Value("${blockchain.gas.price}")
            BigInteger gasPrice,
            @Value("${blockchain.gas.limit}")
            BigInteger gasLimit
    ) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
    }

    private CertificationBenchmark loadContract() {
        return CertificationBenchmark.load(
                contractAddress,
                web3j,
                credentials,
                new StaticGasProvider(gasPrice, gasLimit)
        );
    }

    /*
     * Dùng chung cho hai chiến lược:
     *
     * HASH_PER_RECORD:
     * gọi hàm này N lần với N record hash.
     *
     * MERKLE_BATCHING:
     * gọi hàm này một lần với Merkle root.
     */
    public TransactionReceipt certifyCommitment(
            String commitmentHex
    ) throws Exception {
        byte[] commitment = toBytes32(commitmentHex);

        return loadContract()
                .certifyCommitment(commitment)
                .send();
    }

    /*
     * Baseline phụ: gửi trực tiếp JSON lên blockchain.
     */
    public TransactionReceipt certifyData(
            String rawJson
    ) throws Exception {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException(
                    "rawJson cannot be blank"
            );
        }

        byte[] data = rawJson.getBytes(StandardCharsets.UTF_8);

        return loadContract()
                .certifyData(data)
                .send();
    }

    private byte[] toBytes32(String hex) {
        if (hex == null || hex.isBlank()) {
            throw new IllegalArgumentException(
                    "Commitment cannot be blank"
            );
        }

        String normalized = hex.startsWith("0x")
                ? hex.substring(2)
                : hex;

        if (!normalized.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException(
                    "Commitment must be a 32-byte hexadecimal value"
            );
        }

        return HexUtil.hexToBytes(normalized);
    }

    public String getContractAddress() {
        return contractAddress;
    }
}