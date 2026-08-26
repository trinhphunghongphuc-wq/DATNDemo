package com.service;

import java.util.List;

public interface MerkleService {
    String hashRecord(String rawJson);
    String buildMerkleRoot(List<String> leafHashes);
    List<String> generateProof(List<String> leafHashes, int index);
    boolean verifyProof(String leafHash, List<String> proof, String expectedRoot, int leafIndex);
    List<String> buildMerkleProof(List<String> leafHashes, int targetIndex);
}