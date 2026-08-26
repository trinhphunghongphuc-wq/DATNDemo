package com.entity;

import com.enums.AnchorStatus;
import com.enums.BatchStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "batch_code", unique = true, nullable = false)
    private String batchCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(name = "merkle_root")
    private String merkleRoot;

    @Column(name = "chain_tx_hash")
    private String chainTxHash;

    @JsonFormat(pattern = "HH:mm:ss dd/MM/yyyy")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Record> records;

    @Enumerated(EnumType.STRING)
    @Column(name = "anchor_status")
    private AnchorStatus anchorStatus;

    //Hạn sử dụng
    private LocalDate expiryDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;


    @Column(name = "distributor_id")
    private Long distributorId;

    @Column(name = "retailer_id")
    private Long retailerId;

    //Nghiep vu Retailer
    @Column(name = "total_weight")
    private Double totalWeight;

    @Column(name = "remaining_weight")
    private Double remainingWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BatchStatus.CREATED;
        }
    }

}