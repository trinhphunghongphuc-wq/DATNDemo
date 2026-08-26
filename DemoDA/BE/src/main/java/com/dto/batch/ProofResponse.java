package com.dto.batch;

import lombok.Data;

import java.util.List;

@Data
public class ProofResponse {

    private Long batchId;
    private String recordKey;
    private Integer leafIndex;
    private String leafHash;
    private String merkleRoot;
    private List<String> proof;
    private String chainTxHash;

    public ProofResponse() {
    }

    public ProofResponse(Long batchId, String recordKey, Integer leafIndex,
                         String leafHash, String merkleRoot, List<String> proof, String chainTxHash) {
        this.batchId = batchId;
        this.recordKey = recordKey;
        this.leafIndex = leafIndex;
        this.leafHash = leafHash;
        this.merkleRoot = merkleRoot;
        this.proof = proof;
        this.chainTxHash = chainTxHash;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public Integer getLeafIndex() {
        return leafIndex;
    }

    public void setLeafIndex(Integer leafIndex) {
        this.leafIndex = leafIndex;
    }

    public String getLeafHash() {
        return leafHash;
    }

    public void setLeafHash(String leafHash) {
        this.leafHash = leafHash;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public List<String> getProof() {
        return proof;
    }

    public void setProof(List<String> proof) {
        this.proof = proof;
    }

    public String getChainTxHash() {
        return chainTxHash;
    }

    public void setChainTxHash(String chainTxHash) {
        this.chainTxHash = chainTxHash;
    }
}