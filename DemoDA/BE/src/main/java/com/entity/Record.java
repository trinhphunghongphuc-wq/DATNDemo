package com.entity;

import com.enums.RecordStage;
import com.enums.RecordType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

    // Mã phân biệt từng record.
    @Column(name = "record_key", nullable = false)
    private String recordKey;

    // Dữ liệu JSON gốc của record.
    @Column(name = "raw_json", columnDefinition = "TEXT", nullable = false)
    private String rawJson;

    // CID trỏ tới JSON đã lưu off-chain trên IPFS.
    // Nullable để tương thích các record cũ chưa có CID.
    @Column(name = "ipfs_cid", length = 100)
    private String ipfsCid;

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Mỗi record mới có một salt ngẫu nhiên riêng.
     *
     * Leaf V2 = Keccak-256(saltBytes || canonicalJsonBytes)
     *
     * Nhờ đó, hai record có nội dung JSON giống nhau vẫn tạo leafHash khác nhau,
     * đồng thời hạn chế dò ngược bằng bảng hash dựng sẵn.
     *
     * Nullable để record V1 của Giai đoạn 1 vẫn tiếp tục được xác minh.
     */
    @Column(name = "leaf_salt", length = 64)
    private String leafSalt;

    /*
     * null hoặc 1: V1 hash trực tiếp canonical JSON.
     * 2: V2 salted hash.
     *
     * Không đặt nullable = false vì database đang có record Giai đoạn 1.
     */
    @Column(name = "hash_version")
    private Integer hashVersion;

    // Hash cam kết của record.
    @Column(name = "leaf_hash")
    private String leafHash;

    // Vị trí leaf trong toàn bộ cây Merkle của batch.
    @Column(name = "leaf_index")
    private Integer leafIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_stage")
    private RecordStage recordStage;

    // Vị trí leaf trong cây Merkle riêng của từng giai đoạn.
    @Column(name = "stage_leaf_index")
    private Integer stageLeafIndex;

    @JsonFormat(pattern = "HH:mm:ss dd/MM/yyyy")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Batch chứa record.
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

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Cho biết nội dung trên IPFS là plaintext hay AES-GCM ciphertext.
     *
     * null hoặc false: record cũ/công khai.
     * true: record riêng tư đã được mã hóa.
     */
    @Column(name = "encrypted")
    private Boolean encrypted;

    /*
     * null: không mã hóa.
     * 1: AES-256-GCM envelope phiên bản 1.
     *
     * Nullable để tương thích record Giai đoạn 1.
     */
    @Column(name = "encryption_version")
    private Integer encryptionVersion;
}