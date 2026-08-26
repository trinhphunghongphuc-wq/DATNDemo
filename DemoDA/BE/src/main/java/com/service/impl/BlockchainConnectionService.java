package com.service.impl;


import com.blockchain.contract.TraceabilityRegistry;
import com.service.RecordService;
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
import java.util.Comparator;
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

    private final RecordService recordService;
    private final MerkleServiceImpl merkleService;


    private TraceabilityRegistry loadContract() {
        return TraceabilityRegistry.load(
                contractAddress,
                web3j,
                credentials,
                new StaticGasProvider(gasPrice, gasLimit)
        );
    }

    public String getClientVersion() throws Exception {
        return web3j.web3ClientVersion().send().getWeb3ClientVersion();
    }

    public Long getBlockNumber() throws Exception {
        return web3j.ethBlockNumber().send().getBlockNumber().longValue();
    }

    public  String setRoot(Long batchId, String merkleRootHex) throws Exception {
        TraceabilityRegistry contract = loadContract();
        byte[] rootBytes = HexUtil.hexToBytes(merkleRootHex);

        TransactionReceipt receipt = contract
                .setRoot(BigInteger.valueOf(batchId), rootBytes)
                .send();

        return receipt.getTransactionHash();
    }

    public String getRoot(Long batchId) throws Exception {
        TraceabilityRegistry contract = loadContract();
        byte[] rootBytes = contract
                .getRoot(BigInteger.valueOf(batchId))
                .send();

        return HexUtil.bytesToHex(rootBytes);
    }


    public String getWalletAddress() {
        return credentials.getAddress();
    }
//
//    public boolean verifyRecordProofAgainstBlockchain(Long batchId, String recordKey) {
//        try {
//            com.entity.Record targetRecord = recordService.getByBatchIdAndRecordKey(batchId, recordKey);
//
//            List<com.entity.Record> records = recordService.getRecordsByBatchId(batchId);
//            records.sort(Comparator.comparingInt(Record::getLeafIndex));
//
//            List<String> leafHashes = records.stream()
//                    .map(Record::getLeafHash)
//                    .toList();
//
//            List<String> proof = merkleService.generateProof(leafHashes, targetRecord.getLeafIndex());
//            String onChainRoot = blockchainConnectionService.getRoot(batchId);
//
//            return merkleService.verifyProof(
//                    targetRecord.getLeafHash(),
//                    proof,
//                    onChainRoot,
//                    targetRecord.getLeafIndex()
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to verify against blockchain", e);
//        }
//    }


}