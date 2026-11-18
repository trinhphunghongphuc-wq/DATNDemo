package com.service.impl;

import com.blockchain.contract.TraceabilityRegistry;
import com.blockchain.contract.WalletRegistryCalls;
import com.enums.RecordStage;
import com.entity.Batch;
import com.exception.BusinessException;
import com.repository.BatchRepository;
import org.web3j.abi.EventEncoder;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;
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
    private final BatchRepository batches;

    @Getter
    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Getter
    @Value("${blockchain.wallet-contract.address:}")
    private String walletContractAddress;

    @Value("${blockchain.gas.price}")
    private BigInteger gasPrice;

    @Value("${blockchain.gas.limit}")
    private BigInteger gasLimit;

    private TraceabilityRegistry loadContract(String address) {
        return TraceabilityRegistry.load(
                address,
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
    public synchronized TransactionReceipt anchorStageRoot(
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
                loadContract(contractAddress);

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

        byte[] rootBytes = loadContract(readAddress(batchId))
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

        return loadContract(readAddress(batchId))
                .verifyStageRecord(
                        BigInteger.valueOf(batchId),
                        toBlockchainStage(stage),
                        leafBytes,
                        proofBytes,
                        BigInteger.valueOf(stageLeafIndex)
                )
                .send();
    }

    public BigInteger chainId() throws Exception {
        var response = web3j.ethChainId().send();
        if (response.hasError()) {
            throw new IllegalStateException("Không đọc được chain ID từ Ethereum RPC.");
        }
        return response.getChainId();
    }

    private String readAddress(Long batchId) throws Exception {
        Batch batch = batches.findById(batchId).orElse(null);
        if (batch == null || batch.getWalletRegistryAddress() == null) return contractAddress;
        requireWalletChain(batch);
        return batch.getWalletRegistryAddress();
    }

    public void requireWalletChain(Batch batch) throws Exception {
        if (!chainId().equals(batch.getWalletRegistryChainId())) {
            throw new BusinessException("Ethereum RPC đang trỏ sang mạng khác với mạng của lô.");
        }
    }

    private WalletRegistryCalls walletCalls(String address, BigInteger chainId) {
        return new WalletRegistryCalls(address, web3j,
                new RawTransactionManager(web3j, credentials, chainId.longValueExact()),
                new StaticGasProvider(gasPrice, gasLimit));
    }

    public String stageWallet(String address, BigInteger chain, Long batchId, RecordStage stage) throws Exception {
        return walletCalls(address, chain).stageWallet(batchId, toBlockchainStage(stage).intValueExact());
    }

    public synchronized TransactionReceipt bindBatchWallets(String address, BigInteger chain,
            Long batchId, String producer, String distributor, String retailer) throws Exception {
        return walletCalls(address, chain).bindBatch(batchId, producer, distributor, retailer);
    }

    public boolean checkApproval(Batch batch, RecordStage stage, String root, long deadline, byte[] signature)
            throws Exception {
        requireWalletChain(batch);
        return walletCalls(batch.getWalletRegistryAddress(), batch.getWalletRegistryChainId()).verifyStageApproval(
                batch.getId(), toBlockchainStage(stage).intValueExact(), toBytes32(root, "Root"), deadline, signature);
    }

    public synchronized TransactionReceipt anchorApprovedStage(Batch batch, RecordStage stage,
            String root, long deadline, byte[] signature) throws Exception {
        requireWalletChain(batch);
        return walletCalls(batch.getWalletRegistryAddress(), batch.getWalletRegistryChainId()).anchorApprovedStage(
                batch.getId(), toBlockchainStage(stage).intValueExact(), toBytes32(root, "Root"), deadline, signature);
    }

    // A transaction may have been mined even if the previous HTTP request or database commit failed.
    public TransactionReceipt findWalletAnchorReceipt(Batch batch, RecordStage stage, String root) throws Exception {
        requireWalletChain(batch);
        String address = batch.getWalletRegistryAddress();
        BigInteger batchId = BigInteger.valueOf(batch.getId());
        BigInteger stageId = toBlockchainStage(stage);
        var info = loadContract(address).getStageInfo(batchId, stageId).send();
        if (!info.component5()) return null;
        if (!Numeric.toHexString(info.component1()).equalsIgnoreCase(root)) {
            throw new BusinessException("Root trên chuỗi khác root hiện tại. Hãy đối chiếu trước khi tiếp tục.");
        }
        var block = DefaultBlockParameter.valueOf(info.component3());
        var filter = new EthFilter(block, block, address);
        filter.addSingleTopic(EventEncoder.encode(TraceabilityRegistry.STAGEROOTANCHORED_EVENT));
        filter.addSingleTopic(Numeric.toHexStringWithPrefixZeroPadded(batchId, 64));
        filter.addSingleTopic(Numeric.toHexStringWithPrefixZeroPadded(stageId, 64));
        var logs = web3j.ethGetLogs(filter).send();
        if (logs.hasError() || logs.getLogs().size() != 1) {
            throw new BusinessException("Đã có root trên chuỗi nhưng chưa đọc được giao dịch anchor.");
        }
        var entry = (Log) logs.getLogs().get(0).get();
        return web3j.ethGetTransactionReceipt(entry.getTransactionHash()).send()
                .getTransactionReceipt().orElseThrow(
                        () -> new BusinessException("Chưa đọc được biên nhận anchor từ Ethereum."));
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