package com.dto.product;

import java.util.List;

public class TraceabilityResponse {

    private Long batchId;
    private String batchName;
    private String merkleRoot;
    private String chainTxHash;
    private String status;
    private List<TraceabilityStepResponse> steps;

    public TraceabilityResponse() {
    }

    public TraceabilityResponse(Long batchId, String batchName, String merkleRoot,
                                String chainTxHash, String status, List<TraceabilityStepResponse> steps) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.merkleRoot = merkleRoot;
        this.chainTxHash = chainTxHash;
        this.status = status;
        this.steps = steps;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public String getChainTxHash() {
        return chainTxHash;
    }

    public void setChainTxHash(String chainTxHash) {
        this.chainTxHash = chainTxHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TraceabilityStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<TraceabilityStepResponse> steps) {
        this.steps = steps;
    }
}