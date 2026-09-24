package com.entity;

import com.enums.RecordStage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stage_anchor_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_record_stage",
                        columnNames = {
                                "batch_id",
                                "record_stage"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageAnchorTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "batch_id",
            nullable = false
    )
    @JsonIgnore
    private Batch batch;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "record_stage",
            nullable = false,
            length = 30
    )
    private RecordStage recordStage;

    @Column(
            name = "transaction_hash",
            nullable = false,
            unique = true,
            length = 100
    )
    private String transactionHash;

    @Column(
            name = "block_number",
            nullable = false,
            precision = 78,
            scale = 0
    )
    private BigInteger blockNumber;

    @Column(
            name = "gas_used",
            nullable = false,
            precision = 78,
            scale = 0
    )
    private BigInteger gasUsed;

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Trạng thái anchor được lưu riêng cho từng stage.
     *
     * Producer đã anchor sẽ bị khóa,
     * nhưng Distributor và Retailer vẫn tiếp tục ghi record.
     */
    @Column(
            name = "anchored_at",
            nullable = false
    )
    private LocalDateTime anchoredAt;

    @PrePersist
    public void prePersist() {
        if (anchoredAt == null) {
            anchoredAt = LocalDateTime.now();
        }
    }
}