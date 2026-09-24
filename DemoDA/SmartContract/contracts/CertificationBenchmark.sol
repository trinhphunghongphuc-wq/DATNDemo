// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/*
 * Contract tối giản dùng riêng cho Giai đoạn 3.
 *
 * HASH_PER_RECORD:
 *      gọi certifyCommitment() một lần cho mỗi record hash.
 *
 * MERKLE_BATCHING:
 *      gọi certifyCommitment() một lần cho toàn bộ Merkle root.
 *
 * Cả hai dùng cùng một hàm để phép đo công bằng:
 * khác biệt chính chỉ còn là số lượng transaction.
 */
contract CertificationBenchmark {

    event CommitmentCertified(
        bytes32 indexed commitment,
        address indexed certifiedBy,
        uint256 certifiedAt
    );

    /*
     * Không lưu commitment vào mapping vì mục tiêu benchmark là
     * chứng nhận dữ liệu, không xây dựng thêm nghiệp vụ quản lý.
     *
     * Commitment có thể là:
     * - Hash của một record.
     * - Merkle root của nhiều record.
     */
    function certifyCommitment(bytes32 commitment) external {
        require(commitment != bytes32(0), "Empty commitment");

        emit CommitmentCertified(
            commitment,
            msg.sender,
            block.timestamp
        );
    }

    /*
     * Baseline phụ để đánh giá chi phí đưa trực tiếp JSON on-chain.
     * Chưa cần gọi trong service ở bước đầu.
     */
    event DataCertified(
        bytes data,
        address indexed certifiedBy,
        uint256 certifiedAt
    );

    function certifyData(bytes calldata data) external {
        require(data.length > 0, "Empty data");

        emit DataCertified(
            data,
            msg.sender,
            block.timestamp
        );
    }
}