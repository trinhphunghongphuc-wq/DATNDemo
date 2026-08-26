pragma solidity ^0.8.20;

contract TraceabilityRegistry {

    struct BatchInfo {
        bytes32 merkleRoot;
        uint256 updatedAt;
        address updatedBy;
        bool exists;
    }

    mapping(uint256 => BatchInfo) private batches;

    event RootStored(
        uint256 indexed batchId,
        bytes32 merkleRoot,
        address indexed updatedBy,
        uint256 updatedAt
    );

    function setRoot(uint256 batchId, bytes32 merkleRoot) external {
        require(batchId > 0, "Invalid batchId");
        require(merkleRoot != bytes32(0), "Empty root");

        batches[batchId] = BatchInfo({
            merkleRoot: merkleRoot,
            updatedAt: block.timestamp,
            updatedBy: msg.sender,
            exists: true
        });

        emit RootStored(batchId, merkleRoot, msg.sender, block.timestamp);
    }

    function getRoot(uint256 batchId) external view returns (bytes32) {
        require(batches[batchId].exists, "Batch not found");
        return batches[batchId].merkleRoot;
    }

    function getBatchInfo(uint256 batchId)
        external
        view
        returns (bytes32 merkleRoot, uint256 updatedAt, address updatedBy, bool exists)
    {
        BatchInfo memory batch = batches[batchId];
        return (batch.merkleRoot, batch.updatedAt, batch.updatedBy, batch.exists);
    }

    function verifyRecord(
        uint256 batchId,
        bytes32 leaf,
        bytes32[] calldata proof,
        uint256 leafIndex
    ) external view returns (bool) {
        require(batches[batchId].exists, "Batch not found");

        bytes32 computedHash = leaf;
        uint256 currentIndex = leafIndex;

        for (uint256 i = 0; i < proof.length; i++) {
            bytes32 sibling = proof[i];

            if (currentIndex % 2 == 0) {
                computedHash = keccak256(abi.encodePacked(computedHash, sibling));
            } else {
                computedHash = keccak256(abi.encodePacked(sibling, computedHash));
            }

            currentIndex = currentIndex / 2;
        }

        return computedHash == batches[batchId].merkleRoot;
    }
}