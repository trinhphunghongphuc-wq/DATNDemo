
package com.entity;
import com.enums.RecordType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

}