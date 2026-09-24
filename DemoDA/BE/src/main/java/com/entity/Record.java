
package com.entity;
import com.enums.RecordType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import com.enums.RecordStage;


import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //recordKey: mã phân biệt từng record, ví dụ SP001
    @Column(name = "record_key", nullable = false)
    private String recordKey;

    //rawJson: dữ liệu gốc của record
    @Column(name = "raw_json", columnDefinition = "TEXT", nullable = false)
    private String rawJson;

    // CID trỏ tới bản JSON đã được lưu off-chain trên IPFS.
    // Để nullable trong giai đoạn chuyển đổi vì các record cũ chưa có CID.
    @Column(name = "ipfs_cid", length = 100)
    private String ipfsCid;

    //leafHash: hash của record đó
    @Column(name = "leaf_hash")
    private String leafHash;

    //leafIndex: vị trí lá trong Merkle tree
    @Column(name = "leaf_index")
    private Integer leafIndex;

    @JsonFormat(pattern = "HH:mm:ss dd/MM/yyyy")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    //batch: record này thuộc batch nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnore
    private Batch batch;


    @Column(nullable = false)
    private RecordType recordType;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "record_stage")
    private RecordStage recordStage;

    // Vị trí leaf trong riêng cây Merkle của giai đoạn.
    @Column(name = "stage_leaf_index")
    private Integer stageLeafIndex;

}
