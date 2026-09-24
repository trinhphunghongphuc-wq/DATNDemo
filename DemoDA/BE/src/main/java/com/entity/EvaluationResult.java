package com.entity;

import com.enums.EvaluationModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Các kết quả có cùng runId được tạo từ cùng một bộ dữ liệu,
     * giúp việc so sánh giữa các mô hình công bằng hơn.
     */
    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_model", nullable = false, length = 30)
    private EvaluationModel evaluationModel;

    // Số record được dùng trong lần thử nghiệm.
    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    // Số transaction blockchain mà mô hình cần sử dụng.
    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    // Tổng gas thực tế lấy từ transaction receipt.
    @Column(
            name = "total_gas_used",
            nullable = false,
            precision = 78,
            scale = 0
    )
    private BigInteger totalGasUsed;

    // Tổng số byte được đưa lên blockchain.
    @Column(name = "on_chain_bytes", nullable = false)
    private Long onChainBytes;

    // Dung lượng dữ liệu được giữ ở PostgreSQL/IPFS.
    @Column(name = "off_chain_bytes", nullable = false)
    private Long offChainBytes;

    /*
     * HASH_PER_RECORD: thời gian tạo các hash.
     * MERKLE_BATCHING: thời gian tạo leaf hash và Merkle root.
     */
    @Column(name = "generation_time_ns", nullable = false)
    private Long generationTimeNs;

    /*
     * Thời gian sinh proof theo yêu cầu.
     * Bằng 0 đối với mô hình không sử dụng Merkle proof.
     */
    @Column(name = "proof_generation_time_ns", nullable = false)
    private Long proofGenerationTimeNs;

    /*
     * Thời gian xác minh một record.
     */
    @Column(name = "verification_time_ns", nullable = false)
    private Long verificationTimeNs;

    /*
     * Kích thước proof theo byte.
     * Bằng 0 đối với FULL_ON_CHAIN và HASH_PER_RECORD.
     */
    @Column(name = "proof_size_bytes", nullable = false)
    private Integer proofSizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(
            name = "repetitions",
            nullable = false,
            columnDefinition = "integer default 1"
    )
    private Integer repetitions = 1;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}