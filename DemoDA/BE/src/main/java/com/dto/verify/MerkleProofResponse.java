package com.dto.verify;

import com.enums.RecordStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerkleProofResponse {

    private Long batchId;

    private String recordKey;

    /*
     * Cây Merkle mà record thuộc về:
     * PRODUCER, DISTRIBUTOR hoặc RETAILER.
     */
    private RecordStage recordStage;

    private String leafHash;

    /*
     * Vị trí leaf trong cây Merkle riêng của giai đoạn,
     * không phải vị trí trong toàn bộ batch.
     */
    private Integer stageLeafIndex;

    /*
     * Merkle root của đúng giai đoạn chứa record.
     */
    private String stageRoot;

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Merkle proof không được lưu cố định trong database hoặc IPFS.
     *
     * Proof chỉ được sinh khi API xác minh được gọi,
     * giúp giảm dung lượng lưu trữ và tránh lưu các proof dư thừa.
     */
    private List<String> proof;

    /*
     * Số phần tử hash có trong proof,
     * dùng cho phần đánh giá kích thước proof.
     */
    private Integer proofSize;

    /*
     * Kết quả tự kiểm tra proof trước khi trả về client.
     */
    private boolean valid;
}