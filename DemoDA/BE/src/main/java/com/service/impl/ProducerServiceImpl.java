package com.service.impl;


import com.dto.batch.UpdateExpiryDateRequest;
import com.dto.record.RecordRequest;
import com.dto.user.producer.ProducerBatchRequest;
import com.dto.user.producer.ProducerBatchResponse;
import com.dto.verify.VerifyAllResponse;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;
import com.entity.Batch;
import com.entity.User;
import com.enums.AnchorStatus;
import com.enums.Role;
import com.repository.BatchRepository;
import com.repository.UserRepository;
import com.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.entity.Record;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProducerServiceImpl implements ProducerService {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final BatchService batchService;
    private final RecordService recordService;
    private final VerifyService verifyService;

    @Override
    @Transactional
    public ProducerBatchResponse createBatch(ProducerBatchRequest request, String username) {

        User producer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));


        Batch batch = batchService.createBatchEntity(request, Role.PRODUCER);
        batch.setCreatedBy(producer);
        Batch savedBatch = batchRepository.save(batch);

        return mapToProducerBatchResponse(savedBatch);
    }



    @Override
    @Transactional(readOnly = true)
    public List<ProducerBatchResponse> getMyBatches(String username) {
        return batchRepository.findByCreatedByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::mapToProducerBatchResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProducerBatchResponse getMyBatchDetail(Long batchId, String username) {
        Batch batch = getProducerBatch(batchId, username);
        return mapToProducerBatchResponse(batch);
    }

    @Override
    @Transactional
    public ProducerBatchResponse addRecords(Long batchId, List<RecordRequest> requests, String username) {
        Batch batch = getProducerBatch(batchId, username);

        validateBatchCanBeModified(batch);

        recordService.createRecordsForBatch(batch, requests, Role.PRODUCER);

        Batch updatedBatch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        return mapToProducerBatchResponse(updatedBatch);
    }



    @Override
    @Transactional
    public ProducerBatchResponse updateExpiryDate(
            Long batchId,
            UpdateExpiryDateRequest request,
            String username
    ) {
        Batch batch = getProducerBatch(batchId, username);

        validateBatchCanBeModified(batch);

        batch.setExpiryDate(request.getExpiryDate());

        Batch savedBatch = batchRepository.save(batch);

        return mapToProducerBatchResponse(savedBatch);
    }

    @Override
    @Transactional(readOnly = true)
    public VerifyAllResponse verifyAll(
            Long batchId,
            String username
    ) {
        Batch batch = batchRepository
                .findByIdAndCreatedByUsername(batchId, username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Batch not found or you do not have permission"
                        )
                );

        List<Record> records = batch.getRecords();

        if (records == null || records.isEmpty()) {
            return VerifyAllResponse.builder()
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .batchName(batch.getName())
                    .valid(false)
                    .message("Batch has no records.")
                    .totalRecords(0)
                    .validRecords(0)
                    .invalidRecords(0)
                    .anchorStatus(
                            batch.getAnchorStatus() == null
                                    ? null
                                    : batch.getAnchorStatus().name()
                    )
                    .results(List.of())
                    .build();
        }

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Không đọc plaintext trực tiếp từ PostgreSQL.
         *
         * Chỉ truyền recordKey; VerifyService sẽ:
         * 1. Lấy ciphertext từ IPFS nếu record riêng tư.
         * 2. Giải mã AES-GCM với đúng AAD.
         * 3. Tính lại salted leaf.
         * 4. Xác minh Merkle proof.
         */
        List<VerifyResponse> results = records.stream()
                .map(record -> {
                    VerifyRequest request = new VerifyRequest();
                    request.setBatchId(batch.getId());
                    request.setRecordKey(record.getRecordKey());

                    return verifyService.verify(request);
                })
                .toList();

        int validCount = (int) results.stream()
                .filter(VerifyResponse::isValid)
                .count();

        int totalRecords = results.size();
        int invalidCount = totalRecords - validCount;

        return VerifyAllResponse.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getName())
                .valid(invalidCount == 0)
                .message(invalidCount == 0
                        ? "All records are valid."
                        : "Some records are invalid.")
                .totalRecords(totalRecords)
                .validRecords(validCount)
                .invalidRecords(invalidCount)
                .anchorStatus(
                        batch.getAnchorStatus() == null
                                ? null
                                : batch.getAnchorStatus().name()
                )
                .results(results)
                .build();
    }

    private Batch getProducerBatch(Long batchId, String username) {
        return batchRepository.findByIdAndCreatedByUsername(batchId, username)
                .orElseThrow(() -> new RuntimeException(
                        "Batch not found or you do not have permission to access this batch"
                ));
    }

    private void validateBatchCanBeModified(Batch batch) {
        if (batch.getAnchorStatus() == AnchorStatus.ANCHORED) {
            throw new RuntimeException("Cannot modify anchored batch");
        }
    }

    private ProducerBatchResponse mapToProducerBatchResponse(Batch batch) {
        return ProducerBatchResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .batchCode(batch.getBatchCode())
                .qrContent(
                        "http://localhost:5173/trace/batch/" + batch.getBatchCode()
                )
                .merkleRoot(batch.getMerkleRoot())
                .chainTxHash(batch.getChainTxHash())
                .expiryDate(batch.getExpiryDate())
                .status(batch.getStatus() == null ? null : batch.getStatus().name())
                .anchorStatus(batch.getAnchorStatus() == null ? null : batch.getAnchorStatus().name())
                .createdAt(batch.getCreatedAt())
                .recordCount(batch.getRecords() == null ? 0 : batch.getRecords().size())
                .build();
    }
}