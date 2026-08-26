package com.service.impl;

import com.service.SmartContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class SmartContractServiceImpl implements SmartContractService {

    private final Web3j web3j;
    private final Credentials credentials;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Override
    public String getRoot(Long batchId) throws Exception {
        Function function = new Function(
                "getRoot",
                Collections.singletonList(new Uint256(BigInteger.valueOf(batchId))),
                Collections.singletonList(new TypeReference<Bytes32>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        encodedFunction
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response.getError() != null) {
            throw new RuntimeException("Contract call error: " + response.getError().getMessage());
        }

        if (response.getValue() == null || "0x".equals(response.getValue())) {
            throw new RuntimeException("Empty response from contract");
        }

        var output = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if (output.isEmpty()) {
            throw new RuntimeException("Cannot decode root");
        }

        Bytes32 root = (Bytes32) output.get(0);
        return Numeric.toHexString(root.getValue());
    }

    @Override
    public String setRoot(Long batchId, String merkleRoot) throws Exception {
        validateMerkleRoot(merkleRoot);

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials);
        StaticGasProvider gasProvider = new StaticGasProvider(
                BigInteger.valueOf(20_000_000_000L),
                BigInteger.valueOf(300_000)
        );

        Function function = new Function(
                "setRoot",
                Arrays.asList(
                        new Uint256(BigInteger.valueOf(batchId)),
                        new Bytes32(Numeric.hexStringToByteArray(merkleRoot))
                ),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthSendTransaction ethSendTx = txManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        if (ethSendTx.hasError()) {
            throw new RuntimeException("Send tx error: " + ethSendTx.getError().getMessage());
        }

        String txHash = ethSendTx.getTransactionHash();

        TransactionReceiptProcessor receiptProcessor =
                new PollingTransactionReceiptProcessor(web3j, 1000, 15);

        TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

        if (receipt == null) {
            throw new RuntimeException("Transaction receipt is null");
        }

        if (!isSuccess(receipt.getStatus())) {
            throw new RuntimeException("Blockchain transaction failed. txHash=" + txHash);
        }

        return txHash;
    }

    private void validateMerkleRoot(String merkleRoot) {
        if (merkleRoot == null || !merkleRoot.startsWith("0x")) {
            throw new IllegalArgumentException("Merkle root must start with 0x");
        }

        byte[] bytes = Numeric.hexStringToByteArray(merkleRoot);
        if (bytes.length != 32) {
            throw new IllegalArgumentException("Merkle root must be exactly 32 bytes");
        }
    }

    private boolean isSuccess(String status) {
        return status != null && (
                "0x1".equalsIgnoreCase(status) || "1".equals(status)
        );
    }
}