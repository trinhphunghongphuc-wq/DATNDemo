package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.service.DataEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmDataEncryptionServiceImplTest {

    private DataEncryptionService encryptionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();


        byte[] testKey = new byte[32];

        for (int i = 0; i < testKey.length; i++) {
            testKey[i] = (byte) (i + 1);
        }

        String testKeyBase64 =
                Base64.getEncoder().encodeToString(testKey);

        encryptionService =
                new AesGcmDataEncryptionServiceImpl(
                        testKeyBase64,
                        objectMapper
                );
    }

    @Test
    void encryptThenDecrypt_shouldReturnOriginalJson() {
        String plainJson =
                "{\"deviceId\":\"IOT-DL-001\",\"temperature\":7.5}";

        String context = "batch:8|record:TRAN-001";

        String encryptedJson =
                encryptionService.encrypt(plainJson, context);

        String decryptedJson =
                encryptionService.decrypt(encryptedJson, context);

        assertEquals(plainJson, decryptedJson);
    }

    @Test
    void encrypt_shouldNotExposePlainText() {
        String plainJson =
                "{\"farm\":\"Da Lat Farm\",\"secret\":\"private-data\"}";

        String encryptedJson =
                encryptionService.encrypt(
                        plainJson,
                        "batch:8|record:PROD-001"
                );

        assertNotEquals(plainJson, encryptedJson);
        assertFalse(encryptedJson.contains("Da Lat Farm"));
        assertFalse(encryptedJson.contains("private-data"));
    }

    @Test
    void encryptSameDataTwice_shouldCreateDifferentCiphertexts() {
        String plainJson =
                "{\"temperature\":7.5,\"humidity\":78}";

        String context = "batch:8|record:TRAN-001";

        String encrypted1 =
                encryptionService.encrypt(plainJson, context);

        String encrypted2 =
                encryptionService.encrypt(plainJson, context);


        assertNotEquals(encrypted1, encrypted2);

        assertEquals(
                plainJson,
                encryptionService.decrypt(encrypted1, context)
        );

        assertEquals(
                plainJson,
                encryptionService.decrypt(encrypted2, context)
        );
    }

    @Test
    void decryptWithWrongContext_shouldFailAuthentication() {
        String encryptedJson =
                encryptionService.encrypt(
                        "{\"temperature\":7.5}",
                        "batch:8|record:TRAN-001"
                );


        assertThrows(
                IllegalStateException.class,
                () -> encryptionService.decrypt(
                        encryptedJson,
                        "batch:9|record:TRAN-002"
                )
        );
    }

    @Test
    void decryptTamperedCiphertext_shouldFailAuthentication()
            throws Exception {

        String context = "batch:8|record:TRAN-001";

        String encryptedJson =
                encryptionService.encrypt(
                        "{\"temperature\":7.5}",
                        context
                );

        JsonNode parsed = objectMapper.readTree(encryptedJson);
        ObjectNode envelope = (ObjectNode) parsed;

        String ciphertext =
                envelope.get("ciphertext").asText();

        char replacement =
                ciphertext.charAt(0) == 'A' ? 'B' : 'A';

        String tamperedCiphertext =
                replacement + ciphertext.substring(1);

        envelope.put("ciphertext", tamperedCiphertext);

        String tamperedEnvelope =
                objectMapper.writeValueAsString(envelope);

        assertThrows(
                IllegalStateException.class,
                () -> encryptionService.decrypt(
                        tamperedEnvelope,
                        context
                )
        );
    }
}