package com.service;


import com.dto.verify.MerkleProofResponse;
import com.dto.verify.VerifyAllResponse;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.dto.verify.OnChainVerificationResponse;
public interface VerifyService {
    VerifyResponse verify(VerifyRequest request);

    VerifyAllResponse verifyAllRecords(Long batchId);

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Merkle proof chỉ được tạo khi client yêu cầu.
     *
     * Proof không được lưu trong PostgreSQL hoặc IPFS,
     * giúp giảm dung lượng lưu trữ và tránh proof bị lỗi thời
     * khi cây Merkle của một giai đoạn thay đổi.
     */
    MerkleProofResponse generateMerkleProof(
            Long batchId,
            String recordKey
    );

    /*
     * Xác minh toàn bộ chuỗi:
     * dữ liệu -> salted leaf -> proof -> stage root on-chain.
     */
    OnChainVerificationResponse verifyRecordOnChain(
            Long batchId,
            String recordKey
    );
}