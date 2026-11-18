package com.blockchain.contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.util.List;

public final class WalletRegistryCalls extends Contract {
    public WalletRegistryCalls(String address, Web3j web3j, TransactionManager tx, ContractGasProvider gas) {
        super("", address, web3j, tx, gas);
    }

    public String stageWallet(long batchId, int stage) throws Exception {
        var function = new Function("stageWallet", List.of(new Uint256(batchId), new Uint8(stage)),
                List.of(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class).send();
    }

    public TransactionReceipt bindBatch(long batchId, String producer, String distributor, String retailer) throws Exception {
        var function = new Function("bindBatch",
                List.of(new Uint256(batchId), new Address(producer), new Address(distributor), new Address(retailer)),
                List.of());
        return executeRemoteCallTransaction(function).send();
    }

    public boolean verifyStageApproval(long batchId, int stage, byte[] root, long deadline, byte[] signature)
            throws Exception {
        var function = new Function("verifyStageApproval",
                List.of(new Uint256(batchId), new Uint8(stage), new Bytes32(root), new Uint256(deadline),
                        new DynamicBytes(signature)), List.of(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class).send();
    }

    public TransactionReceipt anchorApprovedStage(long batchId, int stage, byte[] root, long deadline, byte[] signature)
            throws Exception {
        var function = new Function("anchorApprovedStage",
                List.of(new Uint256(batchId), new Uint8(stage), new Bytes32(root), new Uint256(deadline),
                        new DynamicBytes(signature)), List.of());
        return executeRemoteCallTransaction(function).send();
    }
}
