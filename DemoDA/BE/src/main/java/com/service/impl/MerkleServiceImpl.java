package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.MerkleService;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MerkleServiceImpl implements MerkleService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String hashRecord(String rawJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawJson);
            String canonicalJson = objectMapper.writeValueAsString(jsonNode);
            return keccak256(canonicalJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid rawJson format", e);
        }
    }

    @Override
    public String buildMerkleRoot(List<String> leafHashes) {
        validateLeafHashes(leafHashes);

        List<String> currentLevel = new ArrayList<>(leafHashes);

        while (currentLevel.size() > 1) {
            currentLevel = buildNextLevel(currentLevel);
        }

        return currentLevel.get(0);
    }

    @Override
    public List<String> generateProof(List<String> leafHashes, int index) {
        validateLeafHashes(leafHashes);

        if (index < 0 || index >= leafHashes.size()) {
            throw new IllegalArgumentException("Invalid target index");
        }

        List<String> proof = new ArrayList<>();
        List<String> currentLevel = new ArrayList<>(leafHashes);
        int currentIndex = index;

        while (currentLevel.size() > 1) {
            int siblingIndex;

            if (currentIndex % 2 == 0) {
                siblingIndex = (currentIndex + 1 < currentLevel.size()) ? currentIndex + 1 : currentIndex;
            } else {
                siblingIndex = currentIndex - 1;
            }

            proof.add(currentLevel.get(siblingIndex));

            currentLevel = buildNextLevel(currentLevel);
            currentIndex = currentIndex / 2;
        }

        return proof;
    }

    @Override
    public boolean verifyProof(String leafHash, List<String> proof, String root, int targetIndex) {
        String computedHash = leafHash;
        int index = targetIndex;

        for (String siblingHash : proof) {
            if (index % 2 == 0) {
                computedHash = hashPair(computedHash, siblingHash);
            } else {
                computedHash = hashPair(siblingHash, computedHash);
            }
            index = index / 2;
        }

        return computedHash.equals(root);
    }

    @Override
    public List<String> buildMerkleProof(List<String> leafHashes, int targetIndex) {
        return generateProof(leafHashes, targetIndex);
    }

    private List<String> buildNextLevel(List<String> currentLevel) {
        List<String> nextLevel = new ArrayList<>();

        for (int i = 0; i < currentLevel.size(); i += 2) {
            String left = currentLevel.get(i);
            String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
            nextLevel.add(hashPair(left, right));
        }

        return nextLevel;
    }

    private void validateLeafHashes(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            throw new IllegalArgumentException("Leaf hashes cannot be empty");
        }
    }

    private String hashPair(String left, String right) {
        return keccak256(left + right);
    }

    private String keccak256(String input) {
        Keccak.Digest256 digest = new Keccak.Digest256();
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) {
                hex.append('0');
            }
            hex.append(h);
        }
        return hex.toString();
    }

    private byte[] hexToBytes32(String hash) {
        if (hash == null) {
            throw new IllegalArgumentException("Hash must not be null");
        }

        byte[] bytes = HexUtil.hexToBytes(hash);
        if (bytes.length != 32) {
            throw new IllegalArgumentException("Each Merkle hash must be exactly 32 bytes");
        }
        return bytes;
    }
}
