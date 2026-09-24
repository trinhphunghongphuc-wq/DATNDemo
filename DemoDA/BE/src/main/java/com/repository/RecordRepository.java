package com.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.entity.Record;
import com.enums.RecordStage;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordRepository extends JpaRepository<com.entity.Record, Long> {
    List<Record> findByBatchId(Long batchId);
    Record findByRecordKey(String recordKey);
    Optional<Record> findByBatchIdAndRecordKey(Long batchId, String recordKey);
    List<Record> findTop5ByOrderByCreatedAtDesc();
    long countByBatch_MerkleRootIsNotNull();

    @Query("select r.leafHash from Record r where r.batch.id = :batchId order by r.leafIndex asc")
    List<String> findLeafHashesByBatchId(@Param("batchId") Long batchId);

    @Query("select r from Record r where r.batch.id = :batchId order by r.createdAt desc")
    Page<Record> findPageByBatchId(@Param("batchId") Long batchId, Pageable pageable);

    //Verify
    Optional<Record> findByBatchIdAndLeafHash(Long batchId, String leafHash);
    List<Record> findByBatchIdOrderByLeafIndexAsc(Long batchId);
    int countByBatchId(Long batchId);
    int countByBatchIdAndRecordStage(Long batchId, RecordStage recordStage);
    List<Record> findByBatchIdAndRecordStageOrderByStageLeafIndexAsc(
            Long batchId,
            RecordStage recordStage
    );

}
