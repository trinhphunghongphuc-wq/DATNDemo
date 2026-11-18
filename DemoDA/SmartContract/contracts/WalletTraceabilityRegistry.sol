// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./TraceabilityRegistry.sol";

contract WalletTraceabilityRegistry is TraceabilityRegistry {
    bytes32 private constant DOMAIN_TYPE = keccak256(
        "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
    );
    bytes32 private constant APPROVAL_TYPE = keccak256(
        "StageApproval(uint256 batchId,uint8 stage,bytes32 root,uint256 deadline)"
    );
    bytes32 private constant NAME_HASH = keccak256("ChainTrack");
    bytes32 private constant VERSION_HASH = keccak256("2");
    uint256 private constant HALF_CURVE_ORDER =
        0x7fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a0;

    mapping(uint256 => mapping(Stage => address)) public stageWallet;

    event BatchWalletsBound(
        uint256 indexed batchId,
        address producer,
        address distributor,
        address retailer
    );
    event StageApprovalUsed(uint256 indexed batchId, Stage indexed stage, address indexed signer);

    function bindBatch(
        uint256 batchId,
        address producer,
        address distributor,
        address retailer
    ) external onlyOwner {
        require(batchId > 0, "Invalid batchId");
        require(stageWallet[batchId][Stage.PRODUCER] == address(0), "Batch wallets already bound");
        require(producer != address(0) && distributor != address(0) && retailer != address(0), "Missing wallet");
        require(producer != distributor && producer != retailer && distributor != retailer, "Wallets must be distinct");

        require(
            producer != owner && distributor != owner && retailer != owner
                && !authorizedWriters[producer] && !authorizedWriters[distributor] && !authorizedWriters[retailer],
            "Use participant wallets distinct from relayers"
        );

        stageWallet[batchId][Stage.PRODUCER] = producer;
        stageWallet[batchId][Stage.DISTRIBUTOR] = distributor;
        stageWallet[batchId][Stage.RETAILER] = retailer;
        emit BatchWalletsBound(batchId, producer, distributor, retailer);
    }

    // Disable the legacy entry point, including for the owner.
    function anchorStageRoot(uint256, Stage, bytes32) external pure override {
        revert("Stage wallet signature required");
    }

    function verifyStageApproval(
        uint256 batchId,
        Stage stage,
        bytes32 root,
        uint256 deadline,
        bytes calldata signature
    ) public view returns (bool) {
        address assigned = stageWallet[batchId][stage];
        if (assigned == address(0) || block.timestamp > deadline || signature.length != 65) return false;

        bytes32 r = bytes32(signature[:32]);
        bytes32 s = bytes32(signature[32:64]);
        uint8 v = uint8(signature[64]);
        if (v < 27) v += 27;

        if ((v != 27 && v != 28) || uint256(s) > HALF_CURVE_ORDER) return false;

        bytes32 domain = keccak256(abi.encode(
            DOMAIN_TYPE, NAME_HASH, VERSION_HASH, block.chainid, address(this)
        ));
        bytes32 message = keccak256(abi.encode(APPROVAL_TYPE, batchId, uint8(stage), root, deadline));

        return ecrecover(keccak256(abi.encodePacked(hex"1901", domain, message)), v, r, s) == assigned;
    }

    function anchorApprovedStage(
        uint256 batchId,
        Stage stage,
        bytes32 root,
        uint256 deadline,
        bytes calldata signature
    ) external onlyAuthorizedWriter {
        require(verifyStageApproval(batchId, stage, root, deadline, signature), "Invalid stage approval");
        _anchorStageRoot(batchId, stage, root);
        emit StageApprovalUsed(batchId, stage, stageWallet[batchId][stage]);
    }
}
