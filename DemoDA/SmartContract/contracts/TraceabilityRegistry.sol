// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract TraceabilityRegistry {

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Mỗi giai đoạn của chuỗi cung ứng có một Merkle root độc lập.
     *
     * 0 = PRODUCER
     * 1 = DISTRIBUTOR
     * 2 = RETAILER
     */
    enum Stage {
        PRODUCER,
        DISTRIBUTOR,
        RETAILER
    }

    struct StageRootInfo {
        bytes32 merkleRoot;
        uint256 anchoredAt;
        uint256 blockNumber;
        address anchoredBy;
        bool exists;
    }

    address public owner;

    /*
     * Backend hoặc ví blockchain được cấp quyền mới có thể
     * ghi Merkle root lên blockchain.
     */
    mapping(address => bool) public authorizedWriters;

    /*
     * batchId => stage => thông tin root
     */
    mapping(uint256 => mapping(Stage => StageRootInfo))
        private stageRoots;

    /*
     * Đánh dấu batch đã từng có ít nhất một root được anchor.
     */
    mapping(uint256 => bool) private existingBatches;

    event WriterAuthorizationChanged(
        address indexed writer,
        bool authorized
    );

    event StageRootAnchored(
        uint256 indexed batchId,
        Stage indexed stage,
        bytes32 merkleRoot,
        address indexed anchoredBy,
        uint256 anchoredAt,
        uint256 blockNumber
    );

    modifier onlyOwner() {
        require(
            msg.sender == owner,
            "Only owner"
        );
        _;
    }

    modifier onlyAuthorizedWriter() {
        require(
            msg.sender == owner
                || authorizedWriters[msg.sender],
            "Writer is not authorized"
        );
        _;
    }

    constructor() {
        owner = msg.sender;
        authorizedWriters[msg.sender] = true;

        emit WriterAuthorizationChanged(
            msg.sender,
            true
        );
    }

    /*
     * Owner cấp hoặc thu hồi quyền ghi root.
     */
    function setAuthorizedWriter(
        address writer,
        bool authorized
    ) external onlyOwner {
        require(
            writer != address(0),
            "Invalid writer address"
        );

        authorizedWriters[writer] = authorized;

        emit WriterAuthorizationChanged(
            writer,
            authorized
        );
    }

    /*
     * Anchor Merkle root của một giai đoạn.
     *
     * Mỗi stage chỉ được anchor một lần.
     * Điều này ngăn root đã công bố bị ghi đè về sau.
     */
    function anchorStageRoot(
        uint256 batchId,
        Stage stage,
        bytes32 merkleRoot
    ) external onlyAuthorizedWriter {
        require(
            batchId > 0,
            "Invalid batchId"
        );

        require(
            merkleRoot != bytes32(0),
            "Empty Merkle root"
        );

        require(
            !stageRoots[batchId][stage].exists,
            "Stage root already anchored"
        );

        /*
         * State machine on-chain:
         *
         * Distributor chỉ được anchor sau Producer.
         * Retailer chỉ được anchor sau Distributor.
         */
        if (stage == Stage.DISTRIBUTOR) {
            require(
                stageRoots[batchId][Stage.PRODUCER].exists,
                "Producer root is not anchored"
            );
        }

        if (stage == Stage.RETAILER) {
            require(
                stageRoots[batchId][Stage.DISTRIBUTOR].exists,
                "Distributor root is not anchored"
            );
        }

        stageRoots[batchId][stage] = StageRootInfo({
            merkleRoot: merkleRoot,
            anchoredAt: block.timestamp,
            blockNumber: block.number,
            anchoredBy: msg.sender,
            exists: true
        });

        existingBatches[batchId] = true;

        emit StageRootAnchored(
            batchId,
            stage,
            merkleRoot,
            msg.sender,
            block.timestamp,
            block.number
        );
    }

    /*
     * Đọc Merkle root của một stage.
     */
    function getStageRoot(
        uint256 batchId,
        Stage stage
    ) external view returns (bytes32) {
        require(
            stageRoots[batchId][stage].exists,
            "Stage root not found"
        );

        return stageRoots[batchId][stage].merkleRoot;
    }

    /*
     * Đọc đầy đủ thông tin giao dịch anchor của stage.
     */
    function getStageInfo(
        uint256 batchId,
        Stage stage
    )
        external
        view
        returns (
            bytes32 merkleRoot,
            uint256 anchoredAt,
            uint256 blockNumber,
            address anchoredBy,
            bool exists
        )
    {
        StageRootInfo memory info =
            stageRoots[batchId][stage];

        return (
            info.merkleRoot,
            info.anchoredAt,
            info.blockNumber,
            info.anchoredBy,
            info.exists
        );
    }

    function batchExists(
        uint256 batchId
    ) external view returns (bool) {
        return existingBatches[batchId];
    }

    /*
     * Xác minh Merkle proof với root của đúng giai đoạn.
     *
     * Proof được truyền vào khi có yêu cầu,
     * không được lưu trực tiếp trên blockchain.
     */
    function verifyStageRecord(
        uint256 batchId,
        Stage stage,
        bytes32 leaf,
        bytes32[] calldata proof,
        uint256 stageLeafIndex
    ) external view returns (bool) {
        require(
            stageRoots[batchId][stage].exists,
            "Stage root not found"
        );

        bytes32 computedHash = leaf;
        uint256 currentIndex = stageLeafIndex;

        for (uint256 i = 0; i < proof.length; i++) {
            bytes32 sibling = proof[i];

            if (currentIndex % 2 == 0) {
                computedHash = keccak256(
                    abi.encodePacked(
                        computedHash,
                        sibling
                    )
                );
            } else {
                computedHash = keccak256(
                    abi.encodePacked(
                        sibling,
                        computedHash
                    )
                );
            }

            currentIndex = currentIndex / 2;
        }

        return computedHash
            == stageRoots[batchId][stage].merkleRoot;
    }
}