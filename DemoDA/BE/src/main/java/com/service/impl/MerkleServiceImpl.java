package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.service.MerkleService;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class MerkleServiceImpl implements MerkleService {

    private static final int HASH_HEX_LENGTH = 64;

    private final ObjectMapper objectMapper = new ObjectMapper();

    //Update theo Yao va cong su
    private static final int SALT_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String hashRecord(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson cannot be blank");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(rawJson);
            if (jsonNode == null) {
                throw new IllegalArgumentException("rawJson cannot be null JSON");
            }

            JsonNode sortedJson = sortObjectKeysRecursively(jsonNode);
            String canonicalJson = objectMapper.writeValueAsString(sortedJson);
            return keccak256(canonicalJson.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid rawJson format", e);
        }
    }

    @Override
    public String generateSalt() {
        byte[] saltBytes = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);

        StringBuilder saltHex = new StringBuilder(SALT_BYTE_LENGTH * 2);

        for (byte value : saltBytes) {
            saltHex.append(String.format("%02x", value & 0xff));
        }

        return saltHex.toString();
    }

    @Override
    public String hashRecord(String rawJson, String leafSalt) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson cannot be blank");
        }

        if (leafSalt == null || leafSalt.isBlank()) {
            throw new IllegalArgumentException("leafSalt cannot be blank");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(rawJson);

            if (jsonNode == null) {
                throw new IllegalArgumentException("rawJson cannot be null JSON");
            }

            JsonNode sortedJson = sortObjectKeysRecursively(jsonNode);
            String canonicalJson = objectMapper.writeValueAsString(sortedJson);

            // Salt phải chứa đúng 32 byte, tương ứng 64 ký tự hexadecimal.
            String normalizedSalt = normalizeHash(leafSalt);
            byte[] saltBytes = hexToBytes(normalizedSalt);
            byte[] jsonBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

            byte[] saltedPayload =
                    new byte[saltBytes.length + jsonBytes.length];

            System.arraycopy(
                    saltBytes,
                    0,
                    saltedPayload,
                    0,
                    saltBytes.length
            );

            System.arraycopy(
                    jsonBytes,
                    0,
                    saltedPayload,
                    saltBytes.length,
                    jsonBytes.length
            );

            /*
             * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
             * Hash trực tiếp JSON được thay bằng:
             *
             * Leaf V2 = Keccak-256(saltBytes || canonicalJsonBytes)
             *
             * Vì mỗi record có salt ngẫu nhiên riêng nên hai JSON giống nhau
             * cũng không tạo ra cùng một leafHash.
             */
            return keccak256(saltedPayload);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid rawJson format", e);
        }
    }

    @Override
    public String buildMerkleRoot(List<String> leafHashes) {
        validateLeafHashes(leafHashes);

        List<String> currentLevel = normalizeHashes(leafHashes);
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
        List<String> currentLevel = normalizeHashes(leafHashes);
        int currentIndex = index;

        while (currentLevel.size() > 1) {
            int siblingIndex;
            if (currentIndex % 2 == 0) {
                siblingIndex = currentIndex + 1 < currentLevel.size()
                        ? currentIndex + 1
                        : currentIndex;
            } else {
                siblingIndex = currentIndex - 1;
            }

            proof.add(currentLevel.get(siblingIndex));
            currentLevel = buildNextLevel(currentLevel);
            currentIndex /= 2;
        }

        return proof;
    }

    @Override
    public boolean verifyProof(String leafHash, List<String> proof,
                               String expectedRoot, int leafIndex) {
        if (leafIndex < 0) {
            throw new IllegalArgumentException("leafIndex cannot be negative");
        }
        if (proof == null) {
            throw new IllegalArgumentException("proof cannot be null");
        }

        String computedHash = normalizeHash(leafHash);
        String normalizedRoot = normalizeHash(expectedRoot);
        int currentIndex = leafIndex;

        for (String siblingHash : proof) {
            String sibling = normalizeHash(siblingHash);
            computedHash = currentIndex % 2 == 0
                    ? hashPair(computedHash, sibling)
                    : hashPair(sibling, computedHash);
            currentIndex /= 2;
        }

        return computedHash.equals(normalizedRoot);
    }

    @Override
    public List<String> buildMerkleProof(List<String> leafHashes, int targetIndex) {
        return generateProof(leafHashes, targetIndex);
    }

    private JsonNode sortObjectKeysRecursively(JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> sortedFields = new TreeMap<>();
            node.fields().forEachRemaining(entry ->
                    sortedFields.put(entry.getKey(), sortObjectKeysRecursively(entry.getValue())));

            ObjectNode sortedObject = objectMapper.createObjectNode();
            sortedFields.forEach(sortedObject::set);
            return sortedObject;
        }

        if (node.isArray()) {
            ArrayNode sortedArray = objectMapper.createArrayNode();
            node.forEach(child -> sortedArray.add(sortObjectKeysRecursively(child)));
            return sortedArray;
        }

        return node;
    }

    private List<String> buildNextLevel(List<String> currentLevel) {
        List<String> nextLevel = new ArrayList<>((currentLevel.size() + 1) / 2);

        for (int i = 0; i < currentLevel.size(); i += 2) {
            String left = currentLevel.get(i);
            String right = i + 1 < currentLevel.size() ? currentLevel.get(i + 1) : left;
            nextLevel.add(hashPair(left, right));
        }

        return nextLevel;
    }

    private String hashPair(String left, String right) {
        byte[] leftBytes = hexToBytes(normalizeHash(left));
        byte[] rightBytes = hexToBytes(normalizeHash(right));
        byte[] pair = new byte[leftBytes.length + rightBytes.length];

        System.arraycopy(leftBytes, 0, pair, 0, leftBytes.length);
        System.arraycopy(rightBytes, 0, pair, leftBytes.length, rightBytes.length);
        return keccak256(pair);
    }

    private void validateLeafHashes(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            throw new IllegalArgumentException("Leaf hashes cannot be empty");
        }
        leafHashes.forEach(this::normalizeHash);
    }

    private List<String> normalizeHashes(List<String> hashes) {
        return hashes.stream().map(this::normalizeHash).toList();
    }

    private String normalizeHash(String hash) {
        if (hash == null) {
            throw new IllegalArgumentException("Hash cannot be null");
        }

        String normalized = hash.startsWith("0x") || hash.startsWith("0X")
                ? hash.substring(2)
                : hash;

        if (normalized.length() != HASH_HEX_LENGTH || !normalized.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Hash must contain exactly 32 bytes in hexadecimal form");
        }
        return normalized.toLowerCase();
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private String keccak256(byte[] input) {
        Keccak.Digest256 digest = new Keccak.Digest256();
        byte[] hashBytes = digest.digest(input);

        StringBuilder hex = new StringBuilder(HASH_HEX_LENGTH);
        for (byte value : hashBytes) {
            hex.append(String.format("%02x", value & 0xff));
        }
        return hex.toString();
    }
}
