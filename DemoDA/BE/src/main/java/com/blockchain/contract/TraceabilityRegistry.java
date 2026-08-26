package com.blockchain.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.10.3.
 */
@SuppressWarnings("rawtypes")
public class TraceabilityRegistry extends Contract {
    public static final String BINARY = "6080604052348015600e575f5ffd5b506105448061001c5f395ff3fe608060405234801561000f575f5ffd5b506004361061004a575f3560e01c8063048f06f21461004e5780631a0058f5146100635780632f02ebb8146101035780639b24b3b014610126575b5f5ffd5b61006161005c366004610403565b610147565b005b6100c9610071366004610423565b5f90815260208181526040918290208251608081018452815480825260018301549382018490526002909201546001600160a01b038116948201859052600160a01b900460ff16151560609091018190529093919291565b6040516100fa949392919093845260208401929092526001600160a01b031660408301521515606082015260800190565b60405180910390f35b61011661011136600461043a565b610274565b60405190151581526020016100fa565b610139610134366004610423565b610398565b6040519081526020016100fa565b5f821161018d5760405162461bcd60e51b815260206004820152600f60248201526e125b9d985b1a590818985d18da1259608a1b60448201526064015b60405180910390fd5b806101c75760405162461bcd60e51b815260206004820152600a602482015269115b5c1d1e481c9bdbdd60b21b6044820152606401610184565b60408051608081018252828152426020808301828152338486018181526001606087018181525f8b815280875289902097518855935190870155516002909501805492511515600160a01b026001600160a81b03199093166001600160a01b039096169590951791909117909355835185815290810191909152909184917f5edc91f95402a6fdbf0a840493cf2d1deb782566398a0ffd16d2838e4e3b71d1910160405180910390a35050565b5f85815260208190526040812060020154600160a01b900460ff166102cd5760405162461bcd60e51b815260206004820152600f60248201526e10985d18da081b9bdd08199bdd5b99608a1b6044820152606401610184565b84825f5b8581101561037c575f8787838181106102ec576102ec6104c0565b90506020020135905060028361030291906104e8565b5f03610339576040805160208101869052908101829052606001604051602081830303815290604052805190602001209350610366565b60408051602081018390529081018590526060016040516020818303038152906040528051906020012093505b6103716002846104fb565b9250506001016102d1565b50505f8781526020819052604090205414905095945050505050565b5f81815260208190526040812060020154600160a01b900460ff166103f15760405162461bcd60e51b815260206004820152600f60248201526e10985d18da081b9bdd08199bdd5b99608a1b6044820152606401610184565b505f9081526020819052604090205490565b5f5f60408385031215610414575f5ffd5b50508035926020909101359150565b5f60208284031215610433575f5ffd5b5035919050565b5f5f5f5f5f6080868803121561044e575f5ffd5b8535945060208601359350604086013567ffffffffffffffff811115610472575f5ffd5b8601601f81018813610482575f5ffd5b803567ffffffffffffffff811115610498575f5ffd5b8860208260051b84010111156104ac575f5ffd5b959894975060200195606001359392505050565b634e487b7160e01b5f52603260045260245ffd5b634e487b7160e01b5f52601260045260245ffd5b5f826104f6576104f66104d4565b500690565b5f82610509576105096104d4565b50049056fea2646970667358221220df9701273e66a591440aa1ff81fa14dd9fd96b6857cc5e62080c499e6aeb87ad64736f6c634300081e0033";

    public static final String FUNC_GETBATCHINFO = "getBatchInfo";

    public static final String FUNC_GETROOT = "getRoot";

    public static final String FUNC_SETROOT = "setRoot";

    public static final String FUNC_VERIFYRECORD = "verifyRecord";

    public static final Event ROOTSTORED_EVENT = new Event("RootStored", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected TraceabilityRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TraceabilityRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TraceabilityRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TraceabilityRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<RootStoredEventResponse> getRootStoredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROOTSTORED_EVENT, transactionReceipt);
        ArrayList<RootStoredEventResponse> responses = new ArrayList<RootStoredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RootStoredEventResponse typedResponse = new RootStoredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.batchId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.updatedBy = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.merkleRoot = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.updatedAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RootStoredEventResponse getRootStoredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROOTSTORED_EVENT, log);
        RootStoredEventResponse typedResponse = new RootStoredEventResponse();
        typedResponse.log = log;
        typedResponse.batchId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.updatedBy = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.merkleRoot = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.updatedAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<RootStoredEventResponse> rootStoredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRootStoredEventFromLog(log));
    }

    public Flowable<RootStoredEventResponse> rootStoredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROOTSTORED_EVENT));
        return rootStoredEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple4<byte[], BigInteger, String, Boolean>> getBatchInfo(BigInteger batchId) {
        final Function function = new Function(FUNC_GETBATCHINFO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple4<byte[], BigInteger, String, Boolean>>(function,
                new Callable<Tuple4<byte[], BigInteger, String, Boolean>>() {
                    @Override
                    public Tuple4<byte[], BigInteger, String, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<byte[], BigInteger, String, Boolean>(
                                (byte[]) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (Boolean) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<byte[]> getRoot(BigInteger batchId) {
        final Function function = new Function(FUNC_GETROOT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> setRoot(BigInteger batchId, byte[] merkleRoot) {
        final Function function = new Function(
                FUNC_SETROOT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId), 
                new org.web3j.abi.datatypes.generated.Bytes32(merkleRoot)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> verifyRecord(BigInteger batchId, byte[] leaf, List<byte[]> proof, BigInteger leafIndex) {
        final Function function = new Function(FUNC_VERIFYRECORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId), 
                new org.web3j.abi.datatypes.generated.Bytes32(leaf), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(proof, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.generated.Uint256(leafIndex)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static TraceabilityRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TraceabilityRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TraceabilityRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TraceabilityRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TraceabilityRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TraceabilityRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TraceabilityRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TraceabilityRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class RootStoredEventResponse extends BaseEventResponse {
        public BigInteger batchId;

        public String updatedBy;

        public byte[] merkleRoot;

        public BigInteger updatedAt;
    }
}
