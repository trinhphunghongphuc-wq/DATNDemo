package com.blockchain.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
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
public class CertificationBenchmark extends Contract {
    public static final String BINARY = "608060405234801561000f575f80fd5b506104848061001d5f395ff3fe608060405234801561000f575f80fd5b5060043610610034575f3560e01c80631a8469e8146100385780638728007814610054575b5f80fd5b610052600480360381019061004d91906101dc565b610070565b005b61006e60048036038101906100699190610268565b610106565b005b5f801b81036100b4576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016100ab9061030d565b60405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff16817fda560c32d301c59e0a8aae26e613cffe041f9c439f6e067dfeaa5f8ec40895ee426040516100fb9190610343565b60405180910390a350565b5f828290501161014b576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610142906103a6565b60405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff167ff1b7ad1a57fdde9e0b0ae8f0a4847983ee77720de0257b3a5a508465b44844ad8383426040516101959392919061041e565b60405180910390a25050565b5f80fd5b5f80fd5b5f819050919050565b6101bb816101a9565b81146101c5575f80fd5b50565b5f813590506101d6816101b2565b92915050565b5f602082840312156101f1576101f06101a1565b5b5f6101fe848285016101c8565b91505092915050565b5f80fd5b5f80fd5b5f80fd5b5f8083601f84011261022857610227610207565b5b8235905067ffffffffffffffff8111156102455761024461020b565b5b6020830191508360018202830111156102615761026061020f565b5b9250929050565b5f806020838503121561027e5761027d6101a1565b5b5f83013567ffffffffffffffff81111561029b5761029a6101a5565b5b6102a785828601610213565b92509250509250929050565b5f82825260208201905092915050565b7f456d70747920636f6d6d69746d656e74000000000000000000000000000000005f82015250565b5f6102f76010836102b3565b9150610302826102c3565b602082019050919050565b5f6020820190508181035f830152610324816102eb565b9050919050565b5f819050919050565b61033d8161032b565b82525050565b5f6020820190506103565f830184610334565b92915050565b7f456d7074792064617461000000000000000000000000000000000000000000005f82015250565b5f610390600a836102b3565b915061039b8261035c565b602082019050919050565b5f6020820190508181035f8301526103bd81610384565b9050919050565b5f82825260208201905092915050565b828183375f83830152505050565b5f601f19601f8301169050919050565b5f6103fd83856103c4565b935061040a8385846103d4565b610413836103e2565b840190509392505050565b5f6040820190508181035f8301526104378185876103f2565b90506104466020830184610334565b94935050505056fea2646970667358221220714d49f148ef85440d9ac163449f0be82798b06164132958ceb0661c1dae64c764736f6c63430008140033";

    public static final String FUNC_CERTIFYCOMMITMENT = "certifyCommitment";

    public static final String FUNC_CERTIFYDATA = "certifyData";

    public static final Event COMMITMENTCERTIFIED_EVENT = new Event("CommitmentCertified", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event DATACERTIFIED_EVENT = new Event("DataCertified", 
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected CertificationBenchmark(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CertificationBenchmark(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CertificationBenchmark(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CertificationBenchmark(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<CommitmentCertifiedEventResponse> getCommitmentCertifiedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COMMITMENTCERTIFIED_EVENT, transactionReceipt);
        ArrayList<CommitmentCertifiedEventResponse> responses = new ArrayList<CommitmentCertifiedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommitmentCertifiedEventResponse typedResponse = new CommitmentCertifiedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.commitment = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.certifiedBy = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.certifiedAt = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CommitmentCertifiedEventResponse getCommitmentCertifiedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COMMITMENTCERTIFIED_EVENT, log);
        CommitmentCertifiedEventResponse typedResponse = new CommitmentCertifiedEventResponse();
        typedResponse.log = log;
        typedResponse.commitment = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.certifiedBy = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.certifiedAt = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CommitmentCertifiedEventResponse> commitmentCertifiedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCommitmentCertifiedEventFromLog(log));
    }

    public Flowable<CommitmentCertifiedEventResponse> commitmentCertifiedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMITMENTCERTIFIED_EVENT));
        return commitmentCertifiedEventFlowable(filter);
    }

    public static List<DataCertifiedEventResponse> getDataCertifiedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DATACERTIFIED_EVENT, transactionReceipt);
        ArrayList<DataCertifiedEventResponse> responses = new ArrayList<DataCertifiedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DataCertifiedEventResponse typedResponse = new DataCertifiedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.certifiedBy = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.data = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.certifiedAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DataCertifiedEventResponse getDataCertifiedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DATACERTIFIED_EVENT, log);
        DataCertifiedEventResponse typedResponse = new DataCertifiedEventResponse();
        typedResponse.log = log;
        typedResponse.certifiedBy = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.data = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.certifiedAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<DataCertifiedEventResponse> dataCertifiedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDataCertifiedEventFromLog(log));
    }

    public Flowable<DataCertifiedEventResponse> dataCertifiedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DATACERTIFIED_EVENT));
        return dataCertifiedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> certifyCommitment(byte[] commitment) {
        final Function function = new Function(
                FUNC_CERTIFYCOMMITMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(commitment)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> certifyData(byte[] data) {
        final Function function = new Function(
                FUNC_CERTIFYDATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static CertificationBenchmark load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CertificationBenchmark(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CertificationBenchmark load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CertificationBenchmark(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CertificationBenchmark load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CertificationBenchmark(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CertificationBenchmark load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CertificationBenchmark(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<CertificationBenchmark> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CertificationBenchmark.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CertificationBenchmark> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CertificationBenchmark.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<CertificationBenchmark> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CertificationBenchmark.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CertificationBenchmark> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CertificationBenchmark.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class CommitmentCertifiedEventResponse extends BaseEventResponse {
        public byte[] commitment;

        public String certifiedBy;

        public BigInteger certifiedAt;
    }

    public static class DataCertifiedEventResponse extends BaseEventResponse {
        public String certifiedBy;

        public byte[] data;

        public BigInteger certifiedAt;
    }
}
