package com.service;

import java.util.List;

public interface MerkleService {

    // V1: giữ lại để xác minh record cũ của Giai đoạn 1.
    String hashRecord(String rawJson);

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Sinh salt ngẫu nhiên riêng cho từng record.
     */
    String generateSalt();

    /*
     * V2: salted Merkle leaf.
     * Leaf = Keccak-256(saltBytes || canonicalJsonBytes).
     */
    String hashRecord(String rawJson, String leafSalt);

    String buildMerkleRoot(List<String> leafHashes);

    List<String> generateProof(List<String> leafHashes, int index);

    boolean verifyProof(
            String leafHash,
            List<String> proof,
            String expectedRoot,
            int leafIndex
    );

    List<String> buildMerkleProof(
            List<String> leafHashes,
            int targetIndex
    );
}