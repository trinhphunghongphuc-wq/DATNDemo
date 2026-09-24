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
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
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
    public static final String BINARY = "608060405234801561000f575f80fd5b50335f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506001805f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff167f2b4b01750aab6b2b7ff9791c2620de9012fc7b850238362168d734e8e85f150e60016040516100ea9190610111565b60405180910390a261012a565b5f8115159050919050565b61010b816100f7565b82525050565b5f6020820190506101245f830184610102565b92915050565b611673806101375f395ff3fe608060405234801561000f575f80fd5b5060043610610086575f3560e01c80637b4d1fc1116100595780637b4d1fc11461013a5780638da5cb5b1461016a5780639ffaa71e14610188578063ec436337146101b857610086565b806323e7580f1461008a57806346a17346146100be578063526241a8146100ee578063542a086c1461011e575b5f80fd5b6100a4600480360381019061009f9190610cf7565b6101d4565b6040516100b5959493929190610db5565b60405180910390f35b6100d860048036038101906100d39190610cf7565b6102e5565b6040516100e59190610e06565b60405180910390f35b61010860048036038101906101039190610e49565b6103c9565b6040516101159190610e74565b60405180910390f35b61013860048036038101906101339190610eb7565b6103e6565b005b610154600480360381019061014f9190610f80565b610587565b6040516101619190610e74565b60405180910390f35b610172610738565b60405161017f9190611016565b60405180910390f35b6101a2600480360381019061019d919061102f565b61075b565b6040516101af9190610e74565b60405180910390f35b6101d260048036038101906101cd919061105a565b610781565b005b5f805f805f8060025f8981526020019081526020015f205f8860028111156101ff576101fe6110aa565b5b6002811115610211576102106110aa565b5b81526020019081526020015f206040518060a00160405290815f82015481526020016001820154815260200160028201548152602001600382015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020016003820160149054906101000a900460ff1615151515815250509050805f0151816020015182604001518360600151846080015195509550955095509550509295509295909350565b5f60025f8481526020019081526020015f205f83600281111561030b5761030a6110aa565b5b600281111561031d5761031c6110aa565b5b81526020019081526020015f2060030160149054906101000a900460ff1661037a576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161037190611131565b60405180910390fd5b60025f8481526020019081526020015f205f83600281111561039f5761039e6110aa565b5b60028111156103b1576103b06110aa565b5b81526020019081526020015f205f0154905092915050565b6001602052805f5260405f205f915054906101000a900460ff1681565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610473576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161046a90611199565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036104e1576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104d890611201565b60405180910390fd5b8060015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508173ffffffffffffffffffffffffffffffffffffffff167f2b4b01750aab6b2b7ff9791c2620de9012fc7b850238362168d734e8e85f150e8260405161057b9190610e74565b60405180910390a25050565b5f60025f8881526020019081526020015f205f8760028111156105ad576105ac6110aa565b5b60028111156105bf576105be6110aa565b5b81526020019081526020015f2060030160149054906101000a900460ff1661061c576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161061390611131565b60405180910390fd5b5f8590505f8390505f5b868690508110156106e0575f8787838181106106455761064461121f565b5b9050602002013590505f60028461065c9190611279565b036106915783816040516020016106749291906112c9565b6040516020818303038152906040528051906020012093506106bd565b80846040516020016106a49291906112c9565b6040516020818303038152906040528051906020012093505b6002836106ca9190611321565b92505080806106d890611351565b915050610626565b5060025f8a81526020019081526020015f205f896002811115610706576107056110aa565b5b6002811115610718576107176110aa565b5b81526020019081526020015f205f01548214925050509695505050505050565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b5f60035f8381526020019081526020015f205f9054906101000a900460ff169050919050565b5f8054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161480610820575060015f3373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff165b61085f576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610856906113e2565b60405180910390fd5b5f83116108a1576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016108989061144a565b60405180910390fd5b5f801b81036108e5576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016108dc906114b2565b60405180910390fd5b60025f8481526020019081526020015f205f83600281111561090a576109096110aa565b5b600281111561091c5761091b6110aa565b5b81526020019081526020015f2060030160149054906101000a900460ff161561097a576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109719061151a565b60405180910390fd5b6001600281111561098e5761098d6110aa565b5b8260028111156109a1576109a06110aa565b5b03610a3b5760025f8481526020019081526020015f205f8060028111156109cb576109ca6110aa565b5b60028111156109dd576109dc6110aa565b5b81526020019081526020015f2060030160149054906101000a900460ff16610a3a576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610a3190611582565b60405180910390fd5b5b600280811115610a4e57610a4d6110aa565b5b826002811115610a6157610a606110aa565b5b03610afc5760025f8481526020019081526020015f205f60016002811115610a8c57610a8b6110aa565b5b6002811115610a9e57610a9d6110aa565b5b81526020019081526020015f2060030160149054906101000a900460ff16610afb576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610af2906115ea565b60405180910390fd5b5b6040518060a001604052808281526020014281526020014381526020013373ffffffffffffffffffffffffffffffffffffffff1681526020016001151581525060025f8581526020019081526020015f205f846002811115610b6157610b606110aa565b5b6002811115610b7357610b726110aa565b5b81526020019081526020015f205f820151815f015560208201518160010155604082015181600201556060820151816003015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060808201518160030160146101000a81548160ff021916908315150217905550905050600160035f8581526020019081526020015f205f6101000a81548160ff0219169083151502179055503373ffffffffffffffffffffffffffffffffffffffff16826002811115610c5857610c576110aa565b5b847fad442842348a5cac0f74ec915a77b21e89d8e38af0e21b5062681fee76099e41844243604051610c8c93929190611608565b60405180910390a4505050565b5f80fd5b5f80fd5b5f819050919050565b610cb381610ca1565b8114610cbd575f80fd5b50565b5f81359050610cce81610caa565b92915050565b60038110610ce0575f80fd5b50565b5f81359050610cf181610cd4565b92915050565b5f8060408385031215610d0d57610d0c610c99565b5b5f610d1a85828601610cc0565b9250506020610d2b85828601610ce3565b9150509250929050565b5f819050919050565b610d4781610d35565b82525050565b610d5681610ca1565b82525050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610d8582610d5c565b9050919050565b610d9581610d7b565b82525050565b5f8115159050919050565b610daf81610d9b565b82525050565b5f60a082019050610dc85f830188610d3e565b610dd56020830187610d4d565b610de26040830186610d4d565b610def6060830185610d8c565b610dfc6080830184610da6565b9695505050505050565b5f602082019050610e195f830184610d3e565b92915050565b610e2881610d7b565b8114610e32575f80fd5b50565b5f81359050610e4381610e1f565b92915050565b5f60208284031215610e5e57610e5d610c99565b5b5f610e6b84828501610e35565b91505092915050565b5f602082019050610e875f830184610da6565b92915050565b610e9681610d9b565b8114610ea0575f80fd5b50565b5f81359050610eb181610e8d565b92915050565b5f8060408385031215610ecd57610ecc610c99565b5b5f610eda85828601610e35565b9250506020610eeb85828601610ea3565b9150509250929050565b610efe81610d35565b8114610f08575f80fd5b50565b5f81359050610f1981610ef5565b92915050565b5f80fd5b5f80fd5b5f80fd5b5f8083601f840112610f4057610f3f610f1f565b5b8235905067ffffffffffffffff811115610f5d57610f5c610f23565b5b602083019150836020820283011115610f7957610f78610f27565b5b9250929050565b5f805f805f8060a08789031215610f9a57610f99610c99565b5b5f610fa789828a01610cc0565b9650506020610fb889828a01610ce3565b9550506040610fc989828a01610f0b565b945050606087013567ffffffffffffffff811115610fea57610fe9610c9d565b5b610ff689828a01610f2b565b9350935050608061100989828a01610cc0565b9150509295509295509295565b5f6020820190506110295f830184610d8c565b92915050565b5f6020828403121561104457611043610c99565b5b5f61105184828501610cc0565b91505092915050565b5f805f6060848603121561107157611070610c99565b5b5f61107e86828701610cc0565b935050602061108f86828701610ce3565b92505060406110a086828701610f0b565b9150509250925092565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602160045260245ffd5b5f82825260208201905092915050565b7f537461676520726f6f74206e6f7420666f756e640000000000000000000000005f82015250565b5f61111b6014836110d7565b9150611126826110e7565b602082019050919050565b5f6020820190508181035f8301526111488161110f565b9050919050565b7f4f6e6c79206f776e6572000000000000000000000000000000000000000000005f82015250565b5f611183600a836110d7565b915061118e8261114f565b602082019050919050565b5f6020820190508181035f8301526111b081611177565b9050919050565b7f496e76616c6964207772697465722061646472657373000000000000000000005f82015250565b5f6111eb6016836110d7565b91506111f6826111b7565b602082019050919050565b5f6020820190508181035f830152611218816111df565b9050919050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52603260045260245ffd5b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601260045260245ffd5b5f61128382610ca1565b915061128e83610ca1565b92508261129e5761129d61124c565b5b828206905092915050565b5f819050919050565b6112c36112be82610d35565b6112a9565b82525050565b5f6112d482856112b2565b6020820191506112e482846112b2565b6020820191508190509392505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f61132b82610ca1565b915061133683610ca1565b9250826113465761134561124c565b5b828204905092915050565b5f61135b82610ca1565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff820361138d5761138c6112f4565b5b600182019050919050565b7f577269746572206973206e6f7420617574686f72697a656400000000000000005f82015250565b5f6113cc6018836110d7565b91506113d782611398565b602082019050919050565b5f6020820190508181035f8301526113f9816113c0565b9050919050565b7f496e76616c6964206261746368496400000000000000000000000000000000005f82015250565b5f611434600f836110d7565b915061143f82611400565b602082019050919050565b5f6020820190508181035f83015261146181611428565b9050919050565b7f456d707479204d65726b6c6520726f6f740000000000000000000000000000005f82015250565b5f61149c6011836110d7565b91506114a782611468565b602082019050919050565b5f6020820190508181035f8301526114c981611490565b9050919050565b7f537461676520726f6f7420616c726561647920616e63686f72656400000000005f82015250565b5f611504601b836110d7565b915061150f826114d0565b602082019050919050565b5f6020820190508181035f830152611531816114f8565b9050919050565b7f50726f647563657220726f6f74206973206e6f7420616e63686f7265640000005f82015250565b5f61156c601d836110d7565b915061157782611538565b602082019050919050565b5f6020820190508181035f83015261159981611560565b9050919050565b7f4469737472696275746f7220726f6f74206973206e6f7420616e63686f7265645f82015250565b5f6115d46020836110d7565b91506115df826115a0565b602082019050919050565b5f6020820190508181035f830152611601816115c8565b9050919050565b5f60608201905061161b5f830186610d3e565b6116286020830185610d4d565b6116356040830184610d4d565b94935050505056fea2646970667358221220578bcfbe72e7f993bc50adfe6d4658613f5270f844a2c63c96c3eb17d55ea22a64736f6c63430008140033";

    public static final String FUNC_ANCHORSTAGEROOT = "anchorStageRoot";

    public static final String FUNC_AUTHORIZEDWRITERS = "authorizedWriters";

    public static final String FUNC_BATCHEXISTS = "batchExists";

    public static final String FUNC_GETSTAGEINFO = "getStageInfo";

    public static final String FUNC_GETSTAGEROOT = "getStageRoot";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_SETAUTHORIZEDWRITER = "setAuthorizedWriter";

    public static final String FUNC_VERIFYSTAGERECORD = "verifyStageRecord";

    public static final Event STAGEROOTANCHORED_EVENT = new Event("StageRootAnchored", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint8>(true) {}, new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event WRITERAUTHORIZATIONCHANGED_EVENT = new Event("WriterAuthorizationChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Bool>() {}));
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

    public static List<StageRootAnchoredEventResponse> getStageRootAnchoredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(STAGEROOTANCHORED_EVENT, transactionReceipt);
        ArrayList<StageRootAnchoredEventResponse> responses = new ArrayList<StageRootAnchoredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            StageRootAnchoredEventResponse typedResponse = new StageRootAnchoredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.batchId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.stage = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.anchoredBy = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.merkleRoot = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.anchoredAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.blockNumber = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static StageRootAnchoredEventResponse getStageRootAnchoredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(STAGEROOTANCHORED_EVENT, log);
        StageRootAnchoredEventResponse typedResponse = new StageRootAnchoredEventResponse();
        typedResponse.log = log;
        typedResponse.batchId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.stage = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.anchoredBy = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.merkleRoot = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.anchoredAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.blockNumber = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<StageRootAnchoredEventResponse> stageRootAnchoredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getStageRootAnchoredEventFromLog(log));
    }

    public Flowable<StageRootAnchoredEventResponse> stageRootAnchoredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(STAGEROOTANCHORED_EVENT));
        return stageRootAnchoredEventFlowable(filter);
    }

    public static List<WriterAuthorizationChangedEventResponse> getWriterAuthorizationChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(WRITERAUTHORIZATIONCHANGED_EVENT, transactionReceipt);
        ArrayList<WriterAuthorizationChangedEventResponse> responses = new ArrayList<WriterAuthorizationChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            WriterAuthorizationChangedEventResponse typedResponse = new WriterAuthorizationChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.writer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.authorized = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static WriterAuthorizationChangedEventResponse getWriterAuthorizationChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(WRITERAUTHORIZATIONCHANGED_EVENT, log);
        WriterAuthorizationChangedEventResponse typedResponse = new WriterAuthorizationChangedEventResponse();
        typedResponse.log = log;
        typedResponse.writer = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.authorized = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<WriterAuthorizationChangedEventResponse> writerAuthorizationChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getWriterAuthorizationChangedEventFromLog(log));
    }

    public Flowable<WriterAuthorizationChangedEventResponse> writerAuthorizationChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(WRITERAUTHORIZATIONCHANGED_EVENT));
        return writerAuthorizationChangedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> anchorStageRoot(BigInteger batchId, BigInteger stage, byte[] merkleRoot) {
        final Function function = new Function(
                FUNC_ANCHORSTAGEROOT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId), 
                new org.web3j.abi.datatypes.generated.Uint8(stage), 
                new org.web3j.abi.datatypes.generated.Bytes32(merkleRoot)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> authorizedWriters(String param0) {
        final Function function = new Function(FUNC_AUTHORIZEDWRITERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> batchExists(BigInteger batchId) {
        final Function function = new Function(FUNC_BATCHEXISTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Tuple5<byte[], BigInteger, BigInteger, String, Boolean>> getStageInfo(BigInteger batchId, BigInteger stage) {
        final Function function = new Function(FUNC_GETSTAGEINFO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId), 
                new org.web3j.abi.datatypes.generated.Uint8(stage)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple5<byte[], BigInteger, BigInteger, String, Boolean>>(function,
                new Callable<Tuple5<byte[], BigInteger, BigInteger, String, Boolean>>() {
                    @Override
                    public Tuple5<byte[], BigInteger, BigInteger, String, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<byte[], BigInteger, BigInteger, String, Boolean>(
                                (byte[]) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (String) results.get(3).getValue(), 
                                (Boolean) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<byte[]> getStageRoot(BigInteger batchId, BigInteger stage) {
        final Function function = new Function(FUNC_GETSTAGEROOT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId), 
                new org.web3j.abi.datatypes.generated.Uint8(stage)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setAuthorizedWriter(String writer, Boolean authorized) {
        final Function function = new Function(
                FUNC_SETAUTHORIZEDWRITER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, writer), 
                new org.web3j.abi.datatypes.Bool(authorized)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> verifyStageRecord(BigInteger batchId, BigInteger stage, byte[] leaf, List<byte[]> proof, BigInteger stageLeafIndex) {
        final Function function = new Function(FUNC_VERIFYSTAGERECORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(batchId), 
                new org.web3j.abi.datatypes.generated.Uint8(stage), 
                new org.web3j.abi.datatypes.generated.Bytes32(leaf), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(proof, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.generated.Uint256(stageLeafIndex)), 
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

    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TraceabilityRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TraceabilityRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class StageRootAnchoredEventResponse extends BaseEventResponse {
        public BigInteger batchId;

        public BigInteger stage;

        public String anchoredBy;

        public byte[] merkleRoot;

        public BigInteger anchoredAt;

        public BigInteger blockNumber;
    }

    public static class WriterAuthorizationChangedEventResponse extends BaseEventResponse {
        public String writer;

        public Boolean authorized;
    }
}
