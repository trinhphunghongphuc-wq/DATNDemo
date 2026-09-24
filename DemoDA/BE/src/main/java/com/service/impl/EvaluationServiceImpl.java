package com.service.impl;

import com.dto.evaluation.BatchStageEvaluationResponse;
import com.dto.evaluation.EvaluationCostResponse;
import com.entity.EvaluationResult;
import com.entity.StageAnchorTransaction;
import com.enums.EvaluationModel;
import com.repository.EvaluationResultRepository;
import com.repository.RecordRepository;
import com.service.EvaluationService;
import com.service.MerkleService;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import com.entity.Record;
import com.entity.StageAnchorTransaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.repository.RecordRepository;
import com.repository.StageAnchorTransactionRepository;


@Service
public class EvaluationServiceImpl implements EvaluationService {

    /*
     * certifyCommitment(bytes32):
     * 4 byte function selector + 32 byte commitment.
     */
    private static final long COMMITMENT_CALL_BYTES = 36L;

    private static final int WARM_UP_ITERATIONS = 10;

    private final EvaluationResultRepository evaluationResultRepository;
    private final EvaluationBlockchainService evaluationBlockchainService;
    private final MerkleService merkleService;
    private final RecordRepository recordRepository;
    private final StageAnchorTransactionRepository
            stageAnchorTransactionRepository;

    private static final BigDecimal GWEI_PER_ETH =
            new BigDecimal("1000000000");

    public EvaluationServiceImpl(
            EvaluationResultRepository evaluationResultRepository,
            EvaluationBlockchainService evaluationBlockchainService,
            MerkleService merkleService, RecordRepository recordRepository, StageAnchorTransactionRepository stageAnchorTransactionRepository
    ) {
        this.evaluationResultRepository =
                evaluationResultRepository;
        this.evaluationBlockchainService =
                evaluationBlockchainService;
        this.merkleService = merkleService;
        this.recordRepository = recordRepository;
        this.stageAnchorTransactionRepository = stageAnchorTransactionRepository;
    }

    @Override
    public List<EvaluationResult> runComparison(
            int recordCount,
            int repetitions
    ) {
        validateRecordCount(recordCount);
        validateRepetitions(repetitions);

        String runId = UUID.randomUUID().toString();

        /*
         * Cùng một bộ dữ liệu và salt được sử dụng cho các chiến lược
         * nhằm đảm bảo phép so sánh công bằng.
         */
        List<String> rawRecords =
                generateSampleRecords(recordCount);

        List<String> salts =
                generateSalts(recordCount);

        long offChainBytes = rawRecords.stream()
                .mapToLong(value ->
                        value.getBytes(
                                StandardCharsets.UTF_8
                        ).length
                )
                .sum();

        /*
         * Warm-up chỉ thực hiện tính toán local.
         * Không gửi transaction và không lưu kết quả.
         */
        warmUp(rawRecords, salts);

        List<EvaluationResult> fullOnChainMeasurements =
                new ArrayList<>();

        List<EvaluationResult> hashMeasurements =
                new ArrayList<>();

        List<EvaluationResult> merkleMeasurements =
                new ArrayList<>();

        try {
            for (int iteration = 0;
                 iteration < repetitions;
                 iteration++) {

                /*
                 * Luân phiên Hash và Merkle để tránh một chiến lược
                 * luôn được chạy trước chiến lược còn lại.
                 */
                if (iteration % 2 == 0) {
                    hashMeasurements.add(
                            evaluateHashPerRecord(
                                    runId,
                                    rawRecords,
                                    salts,
                                    offChainBytes
                            )
                    );

                    merkleMeasurements.add(
                            evaluateMerkleBatching(
                                    runId,
                                    rawRecords,
                                    salts,
                                    offChainBytes
                            )
                    );
                } else {
                    merkleMeasurements.add(
                            evaluateMerkleBatching(
                                    runId,
                                    rawRecords,
                                    salts,
                                    offChainBytes
                            )
                    );

                    hashMeasurements.add(
                            evaluateHashPerRecord(
                                    runId,
                                    rawRecords,
                                    salts,
                                    offChainBytes
                            )
                    );
                }

                /*
                 * Full on-chain chỉ là baseline đánh giá chi phí.
                 * Không tham gia phép đo thời gian hash/Merkle.
                 */
                fullOnChainMeasurements.add(
                        evaluateFullOnChain(
                                runId,
                                rawRecords
                        )
                );
            }

            EvaluationResult fullOnChainAverage =
                    averageMeasurements(
                            runId,
                            EvaluationModel.FULL_ON_CHAIN,
                            fullOnChainMeasurements,
                            repetitions
                    );

            EvaluationResult hashAverage =
                    averageMeasurements(
                            runId,
                            EvaluationModel.HASH_PER_RECORD,
                            hashMeasurements,
                            repetitions
                    );

            EvaluationResult merkleAverage =
                    averageMeasurements(
                            runId,
                            EvaluationModel.MERKLE_BATCHING,
                            merkleMeasurements,
                            repetitions
                    );

            return evaluationResultRepository.saveAll(
                    List.of(
                            fullOnChainAverage,
                            hashAverage,
                            merkleAverage
                    )
            );
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Cannot run blockchain evaluation",
                    exception
            );
        }
    }

    /*
     * Warm-up giúp giảm ảnh hưởng của class loading và JIT compiler.
     * Cả Hash và Merkle đều dùng cùng dữ liệu trong warm-up.
     */
    private void warmUp(
            List<String> rawRecords,
            List<String> salts
    ) {
        int targetIndex = rawRecords.size() / 2;

        for (int i = 0;
             i < WARM_UP_ITERATIONS;
             i++) {

            List<String> hashes =
                    hashRecords(rawRecords, salts);

            String root =
                    merkleService.buildMerkleRoot(hashes);

            List<String> proof =
                    merkleService.generateProof(
                            hashes,
                            targetIndex
                    );

            boolean valid = merkleService.verifyProof(
                    hashes.get(targetIndex),
                    proof,
                    root,
                    targetIndex
            );

            if (!valid) {
                throw new IllegalStateException(
                        "Merkle warm-up verification failed"
                );
            }
        }
    }

    /*
     * Baseline phụ:
     * mỗi JSON được gửi nguyên văn trong một transaction.
     */
    private EvaluationResult evaluateFullOnChain(
            String runId,
            List<String> rawRecords
    ) throws Exception {

        BigInteger totalGasUsed = BigInteger.ZERO;
        long calldataBytes = 0L;

        for (String rawJson : rawRecords) {
            TransactionReceipt receipt =
                    evaluationBlockchainService
                            .certifyData(rawJson);

            totalGasUsed = totalGasUsed.add(
                    receipt.getGasUsed()
            );

            calldataBytes +=
                    calculateDynamicBytesCallSize(rawJson);
        }

        return EvaluationResult.builder()
                .runId(runId)
                .evaluationModel(
                        EvaluationModel.FULL_ON_CHAIN
                )
                .recordCount(rawRecords.size())
                .repetitions(1)
                .transactionCount(rawRecords.size())
                .totalGasUsed(totalGasUsed)
                .onChainBytes(calldataBytes)

                /*
                 * Baseline giả định toàn bộ JSON được công bố on-chain.
                 * Vì vậy không tính dung lượng kho off-chain.
                 */
                .offChainBytes(0L)

                /*
                 * Full on-chain không tạo hash, cây Merkle hoặc proof.
                 * Baseline này tập trung vào gas và calldata.
                 */
                .generationTimeNs(0L)
                .proofGenerationTimeNs(0L)
                .verificationTimeNs(0L)
                .proofSizeBytes(0)
                .build();
    }

    /*
     * HASH_PER_RECORD:
     * mỗi record được hash và gửi bằng một transaction riêng.
     */
    private EvaluationResult evaluateHashPerRecord(
            String runId,
            List<String> rawRecords,
            List<String> salts,
            long offChainBytes
    ) throws Exception {

        long generationStart = System.nanoTime();

        List<String> leafHashes =
                hashRecords(rawRecords, salts);

        long generationTime =
                System.nanoTime() - generationStart;

        BigInteger totalGasUsed = BigInteger.ZERO;

        for (String leafHash : leafHashes) {
            TransactionReceipt receipt =
                    evaluationBlockchainService
                            .certifyCommitment(leafHash);

            totalGasUsed = totalGasUsed.add(
                    receipt.getGasUsed()
            );
        }

        int targetIndex = rawRecords.size() / 2;

        long verificationStart = System.nanoTime();

        String recalculatedHash = merkleService.hashRecord(
                rawRecords.get(targetIndex),
                salts.get(targetIndex)
        );

        boolean valid = recalculatedHash.equals(
                leafHashes.get(targetIndex)
        );

        long verificationTime =
                System.nanoTime() - verificationStart;

        if (!valid) {
            throw new IllegalStateException(
                    "Hash-per-record verification failed"
            );
        }

        return EvaluationResult.builder()
                .runId(runId)
                .evaluationModel(
                        EvaluationModel.HASH_PER_RECORD
                )
                .recordCount(rawRecords.size())
                .repetitions(1)
                .transactionCount(rawRecords.size())
                .totalGasUsed(totalGasUsed)
                .onChainBytes(
                        COMMITMENT_CALL_BYTES
                                * rawRecords.size()
                )
                .offChainBytes(offChainBytes)
                .generationTimeNs(generationTime)
                .proofGenerationTimeNs(0L)
                .verificationTimeNs(verificationTime)
                .proofSizeBytes(0)
                .build();
    }

    /*
     * MERKLE_BATCHING:
     * nhiều record được gom thành một Merkle root
     * và chỉ cần một blockchain transaction.
     */
    private EvaluationResult evaluateMerkleBatching(
            String runId,
            List<String> rawRecords,
            List<String> salts,
            long offChainBytes
    ) throws Exception {

        long generationStart = System.nanoTime();

        List<String> leafHashes =
                hashRecords(rawRecords, salts);

        String merkleRoot =
                merkleService.buildMerkleRoot(leafHashes);

        long generationTime =
                System.nanoTime() - generationStart;

        TransactionReceipt receipt =
                evaluationBlockchainService
                        .certifyCommitment(merkleRoot);

        int targetIndex = rawRecords.size() / 2;

        long proofStart = System.nanoTime();

        List<String> proof =
                merkleService.generateProof(
                        leafHashes,
                        targetIndex
                );

        long proofGenerationTime =
                System.nanoTime() - proofStart;

        long verificationStart = System.nanoTime();

        boolean valid = merkleService.verifyProof(
                leafHashes.get(targetIndex),
                proof,
                merkleRoot,
                targetIndex
        );

        long verificationTime =
                System.nanoTime() - verificationStart;

        if (!valid) {
            throw new IllegalStateException(
                    "Merkle proof verification failed"
            );
        }

        return EvaluationResult.builder()
                .runId(runId)
                .evaluationModel(
                        EvaluationModel.MERKLE_BATCHING
                )
                .recordCount(rawRecords.size())
                .repetitions(1)
                .transactionCount(1)
                .totalGasUsed(receipt.getGasUsed())
                .onChainBytes(COMMITMENT_CALL_BYTES)
                .offChainBytes(offChainBytes)
                .generationTimeNs(generationTime)
                .proofGenerationTimeNs(
                        proofGenerationTime
                )
                .verificationTimeNs(verificationTime)
                .proofSizeBytes(proof.size() * 32)
                .build();
    }

    /*
     * Tính kết quả trung bình từ nhiều lần chạy cùng chiến lược.
     */
    private EvaluationResult averageMeasurements(
            String runId,
            EvaluationModel model,
            List<EvaluationResult> measurements,
            int repetitions
    ) {
        if (measurements.isEmpty()) {
            throw new IllegalArgumentException(
                    "Measurements cannot be empty"
            );
        }

        EvaluationResult first = measurements.get(0);

        BigInteger gasSum = measurements.stream()
                .map(EvaluationResult::getTotalGasUsed)
                .reduce(
                        BigInteger.ZERO,
                        BigInteger::add
                );

        long generationTimeSum = measurements.stream()
                .mapToLong(
                        EvaluationResult::getGenerationTimeNs
                )
                .sum();

        long proofTimeSum = measurements.stream()
                .mapToLong(
                        EvaluationResult::
                                getProofGenerationTimeNs
                )
                .sum();

        long verificationTimeSum = measurements.stream()
                .mapToLong(
                        EvaluationResult::getVerificationTimeNs
                )
                .sum();

        return EvaluationResult.builder()
                .runId(runId)
                .evaluationModel(model)
                .recordCount(first.getRecordCount())
                .repetitions(repetitions)

                /*
                 * Số transaction cần trong một lần thử nghiệm,
                 * không nhân với số repetitions.
                 */
                .transactionCount(
                        first.getTransactionCount()
                )

                /*
                 * Tổng gas trung bình của một lần chạy chiến lược.
                 */
                .totalGasUsed(
                        gasSum.divide(
                                BigInteger.valueOf(repetitions)
                        )
                )
                .onChainBytes(first.getOnChainBytes())
                .offChainBytes(first.getOffChainBytes())
                .generationTimeNs(
                        generationTimeSum / repetitions
                )
                .proofGenerationTimeNs(
                        proofTimeSum / repetitions
                )
                .verificationTimeNs(
                        verificationTimeSum / repetitions
                )
                .proofSizeBytes(first.getProofSizeBytes())
                .build();
    }

    /*
     * Kích thước ABI calldata của certifyData(bytes):
     *
     * 4 byte: function selector.
     * 32 byte: offset.
     * 32 byte: độ dài.
     * N byte: nội dung JSON được padding thành bội số của 32.
     */
    private long calculateDynamicBytesCallSize(
            String rawJson
    ) {
        int dataLength = rawJson
                .getBytes(StandardCharsets.UTF_8)
                .length;

        long paddedDataLength =
                ((dataLength + 31L) / 32L) * 32L;

        return 4L
                + 32L
                + 32L
                + paddedDataLength;
    }

    private List<String> hashRecords(
            List<String> rawRecords,
            List<String> salts
    ) {
        List<String> hashes =
                new ArrayList<>(rawRecords.size());

        for (int i = 0;
             i < rawRecords.size();
             i++) {

            hashes.add(
                    merkleService.hashRecord(
                            rawRecords.get(i),
                            salts.get(i)
                    )
            );
        }

        return hashes;
    }

    private List<String> generateSalts(
            int recordCount
    ) {
        List<String> salts =
                new ArrayList<>(recordCount);

        for (int i = 0;
             i < recordCount;
             i++) {

            salts.add(
                    merkleService.generateSalt()
            );
        }

        return salts;
    }

    private List<String> generateSampleRecords(
            int recordCount
    ) {
        List<String> records =
                new ArrayList<>(recordCount);

        for (int i = 0;
             i < recordCount;
             i++) {

            records.add(
                    """
                    {
                      "recordNumber": %d,
                      "product": "Ca Chua Da Lat",
                      "temperature": %.1f,
                      "humidity": %.1f,
                      "quantity": "%dkg"
                    }
                    """.formatted(
                            i + 1,
                            4.0 + (i % 7),
                            70.0 + (i % 15),
                            10 + i
                    )
            );
        }

        return records;
    }

    private void validateRecordCount(
            int recordCount
    ) {
        if (recordCount < 1 ||
                recordCount > 1000) {

            throw new IllegalArgumentException(
                    "recordCount must be between 1 and 1000"
            );
        }
    }

    private void validateRepetitions(
            int repetitions
    ) {
        if (repetitions < 1 ||
                repetitions > 10) {

            throw new IllegalArgumentException(
                    "repetitions must be between 1 and 10"
            );
        }
    }

    @Override
    public List<EvaluationResult> getResultsByRunId(
            String runId
    ) {
        return evaluationResultRepository
                .findByRunIdOrderByEvaluationModelAsc(
                        runId
                );
    }

    @Override
    public List<EvaluationResult> getAllResults() {
        return evaluationResultRepository
                .findAllByOrderByCreatedAtDesc();
    }

    @Override
    public BatchStageEvaluationResponse evaluateBatchStageModel(
            Long batchId
    ) {
        List<Record> records =
                recordRepository.findByBatchId(batchId);

        if (records.isEmpty()) {
            throw new IllegalArgumentException(
                    "Batch has no records or does not exist: "
                            + batchId
            );
        }

        int totalRecords = records.size();

        int stagesWithRecords = (int) records.stream()
                .map(Record::getRecordStage)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        List<StageAnchorTransaction> anchors =
                stageAnchorTransactionRepository
                        .findByBatch_IdOrderByRecordStageAsc(
                                batchId
                        );

        BigInteger actualStageGasUsed = anchors.stream()
                .map(StageAnchorTransaction::getGasUsed)
                .filter(Objects::nonNull)
                .reduce(
                        BigInteger.ZERO,
                        BigInteger::add
                );

        double transactionReductionPercent =
                totalRecords == 0
                        ? 0.0
                        : (
                        1.0
                                - (
                                (double) stagesWithRecords
                                        / totalRecords
                        )
                ) * 100.0;

        return BatchStageEvaluationResponse.builder()
                .batchId(batchId)
                .totalRecords(totalRecords)
                .stagesWithRecords(stagesWithRecords)
                .hashPerRecordTransactions(totalRecords)
                .stageMerkleTransactions(stagesWithRecords)
                .anchoredStages(anchors.size())
                .actualStageGasUsed(actualStageGasUsed)
                .transactionReductionPercent(
                        transactionReductionPercent
                )
                .fullyAnchored(
                        anchors.size() == stagesWithRecords
                )
                .build();
    }

    @Override
    public List<EvaluationCostResponse> estimateRunCosts(
            String runId,
            BigDecimal l1GasPriceGwei,
            BigDecimal l2GasPriceGwei,
            BigDecimal ethPriceUsd
    ) {
        validateCostParameters(
                l1GasPriceGwei,
                l2GasPriceGwei,
                ethPriceUsd
        );

        List<EvaluationResult> results =
                evaluationResultRepository
                        .findByRunIdOrderByEvaluationModelAsc(
                                runId
                        );

        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "Evaluation run not found: " + runId
            );
        }

        return results.stream()
                .map(result ->
                        buildCostResponse(
                                result,
                                l1GasPriceGwei,
                                l2GasPriceGwei,
                                ethPriceUsd
                        )
                )
                .toList();
    }

    private EvaluationCostResponse buildCostResponse(
            EvaluationResult result,
            BigDecimal l1GasPriceGwei,
            BigDecimal l2GasPriceGwei,
            BigDecimal ethPriceUsd
    ) {
        BigDecimal l1CostEth = calculateCostEth(
                result.getTotalGasUsed(),
                l1GasPriceGwei
        );

        BigDecimal l2CostEth = calculateCostEth(
                result.getTotalGasUsed(),
                l2GasPriceGwei
        );

        BigDecimal l1CostUsd = l1CostEth
                .multiply(ethPriceUsd)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal l2CostUsd = l2CostEth
                .multiply(ethPriceUsd)
                .setScale(6, RoundingMode.HALF_UP);

        return EvaluationCostResponse.builder()
                .runId(result.getRunId())
                .evaluationModel(
                        result.getEvaluationModel()
                )
                .recordCount(result.getRecordCount())
                .transactionCount(
                        result.getTransactionCount()
                )
                .totalGasUsed(result.getTotalGasUsed())
                .l1GasPriceGwei(l1GasPriceGwei)
                .l1CostEth(l1CostEth)
                .l1CostUsd(l1CostUsd)
                .l2GasPriceGwei(l2GasPriceGwei)
                .l2ExecutionCostEth(l2CostEth)
                .l2ExecutionCostUsd(l2CostUsd)
                .ethPriceUsd(ethPriceUsd)
                .note(
                        "L2 value estimates execution fee only; "
                                + "rollup L1 data fee is not included."
                )
                .build();
    }

    private BigDecimal calculateCostEth(
            BigInteger gasUsed,
            BigDecimal gasPriceGwei
    ) {
        return new BigDecimal(gasUsed)
                .multiply(gasPriceGwei)
                .divide(
                        GWEI_PER_ETH,
                        18,
                        RoundingMode.HALF_UP
                );
    }

    private void validateCostParameters(
            BigDecimal l1GasPriceGwei,
            BigDecimal l2GasPriceGwei,
            BigDecimal ethPriceUsd
    ) {
        if (l1GasPriceGwei == null ||
                l1GasPriceGwei.signum() < 0) {
            throw new IllegalArgumentException(
                    "l1GasPriceGwei must be non-negative"
            );
        }

        if (l2GasPriceGwei == null ||
                l2GasPriceGwei.signum() < 0) {
            throw new IllegalArgumentException(
                    "l2GasPriceGwei must be non-negative"
            );
        }

        if (ethPriceUsd == null ||
                ethPriceUsd.signum() < 0) {
            throw new IllegalArgumentException(
                    "ethPriceUsd must be non-negative"
            );
        }
    }
}