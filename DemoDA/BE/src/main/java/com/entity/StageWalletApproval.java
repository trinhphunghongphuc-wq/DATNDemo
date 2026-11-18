package com.entity;

import com.enums.RecordStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stage_wallet_approvals")
@Getter
@Setter
@NoArgsConstructor
public class StageWalletApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecordStage stage;

    @Column(nullable = false, length = 42)
    private String walletAddress;

    @Column(nullable = false, length = 66)
    private String merkleRoot;

    @Column(nullable = false)
    private long deadline;

    @Column(nullable = false, length = 132)
    private String signature;
}
