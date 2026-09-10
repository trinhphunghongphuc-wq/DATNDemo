package com.service.impl;

import com.service.MerkleService;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

class MerkleServiceImplTest {

    private MerkleService merkleService;

    @BeforeEach
    void setUp() {
        merkleService = new MerkleServiceImpl();
    }

    @Test
    void generateSalt_shouldReturnRandom32ByteHex() {
        String salt1 = merkleService.generateSalt();
        String salt2 = merkleService.generateSalt();

        // 32 byte = 64 ký tự hexadecimal.
        assertNotNull(salt1);
        Assert.assertEquals(64, salt1.length());
        Assert.assertTrue(salt1.matches("[0-9a-f]{64}"));

        assertNotNull(salt2);
        Assert.assertEquals(64, salt2.length());
        Assert.assertTrue(salt2.matches("[0-9a-f]{64}"));

        // Hai record phải có salt riêng.
        Assert.assertNotEquals(salt1, salt2);
    }

    @Test
    void saltedHash_sameJsonAndSameSalt_shouldReturnSameHash() {
        String rawJson =
                "{\"farm\":\"Da Lat Farm\",\"quantity\":100}";

        String salt = merkleService.generateSalt();

        String hash1 = merkleService.hashRecord(rawJson, salt);
        String hash2 = merkleService.hashRecord(rawJson, salt);

        Assert.assertEquals(hash1, hash2);
        Assert.assertEquals(64, hash1.length());
    }

    @Test
    void saltedHash_sameJsonButDifferentSalt_shouldReturnDifferentHashes() {
        String rawJson =
                "{\"farm\":\"Da Lat Farm\",\"quantity\":100}";

        String salt1 = merkleService.generateSalt();
        String salt2 = merkleService.generateSalt();

        String hash1 = merkleService.hashRecord(rawJson, salt1);
        String hash2 = merkleService.hashRecord(rawJson, salt2);

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Dữ liệu giống nhau nhưng salt khác nhau phải tạo leafHash khác nhau.
         * Điều này hạn chế việc nhận biết hai record có cùng nội dung.
         */
        Assert.assertNotEquals(hash1, hash2);
    }

    @Test
    void saltedHash_differentJsonKeyOrder_shouldReturnSameHash() {
        String json1 =
                "{\"farm\":\"Da Lat Farm\",\"quantity\":100}";

        String json2 =
                "{\"quantity\":100,\"farm\":\"Da Lat Farm\"}";

        String salt = merkleService.generateSalt();

        String hash1 = merkleService.hashRecord(json1, salt);
        String hash2 = merkleService.hashRecord(json2, salt);

        // Canonical JSON bảo đảm thứ tự key không làm thay đổi hash.
        Assert.assertEquals(hash1, hash2);
    }

    @Test
    void legacyHash_shouldRemainCompatibleWithPhase1() {
        String json1 =
                "{\"farm\":\"Da Lat Farm\",\"quantity\":100}";

        String json2 =
                "{\"quantity\":100,\"farm\":\"Da Lat Farm\"}";

        String hash1 = merkleService.hashRecord(json1);
        String hash2 = merkleService.hashRecord(json2);

        // Hàm V1 vẫn hoạt động cho record cũ.
        Assert.assertEquals(hash1, hash2);
    }

    @Test
    void merkleProof_shouldWorkWithSaltedLeafHashes() {
        String json1 = "{\"record\":\"PRODUCTION\"}";
        String json2 = "{\"record\":\"TRANSPORT\"}";
        String json3 = "{\"record\":\"RETAIL\"}";

        String leaf1 = merkleService.hashRecord(
                json1,
                merkleService.generateSalt()
        );

        String leaf2 = merkleService.hashRecord(
                json2,
                merkleService.generateSalt()
        );

        String leaf3 = merkleService.hashRecord(
                json3,
                merkleService.generateSalt()
        );

        List<String> leaves = List.of(leaf1, leaf2, leaf3);

        String root = merkleService.buildMerkleRoot(leaves);
        List<String> proof = merkleService.generateProof(leaves, 1);

        boolean valid = merkleService.verifyProof(
                leaf2,
                proof,
                root,
                1
        );

        Assert.assertTrue(valid);
    }

    @Test
    void saltedHash_invalidSalt_shouldThrowException() {
        String rawJson = "{\"farm\":\"Da Lat Farm\"}";

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> merkleService.hashRecord(rawJson, "invalid-salt")
        );
    }
}