package com.service.impl;

import com.entity.Batch;
import com.entity.Record;
import com.enums.RecordStage;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.service.MerkleService;
import com.service.StageMerkleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StageMerkleServiceImpl implements StageMerkleService {

    private final RecordRepository recordRepository;
    private final BatchRepository batchRepository;
    private final MerkleService merkleService;

    public StageMerkleServiceImpl(
            RecordRepository recordRepository,
            BatchRepository batchRepository,
            MerkleService merkleService
    ) {
        this.recordRepository = recordRepository;
        this.batchRepository = batchRepository;
        this.merkleService = merkleService;
    }

    @Override
    public String refreshStageRoot(
            Batch batch,
            RecordStage recordStage
    ) {
        if (batch == null || batch.getId() == null) {
            throw new IllegalArgumentException(
                    "Saved batch is required"
            );
        }

        if (recordStage == null) {
            throw new IllegalArgumentException(
                    "recordStage is required"
            );
        }

        List<String> leafHashes = recordRepository
                .findByBatchIdAndRecordStageOrderByStageLeafIndexAsc(
                        batch.getId(),
                        recordStage
                )
                .stream()
                .map(Record::getLeafHash)
                .toList();

        if (leafHashes.isEmpty()) {
            throw new IllegalStateException(
                    "No records found for stage " + recordStage
            );
        }

        String stageRoot =
                merkleService.buildMerkleRoot(leafHashes);

        switch (recordStage) {
            case PRODUCER ->
                    batch.setProducerMerkleRoot(stageRoot);

            case DISTRIBUTOR ->
                    batch.setDistributorMerkleRoot(stageRoot);

            case RETAILER ->
                    batch.setRetailerMerkleRoot(stageRoot);
        }

        batchRepository.save(batch);
        return stageRoot;
    }
}