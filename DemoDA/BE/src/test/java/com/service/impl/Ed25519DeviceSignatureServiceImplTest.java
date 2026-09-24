package com.service.impl;

import com.service.DeviceSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class Ed25519DeviceSignatureServiceImplTest {

    private DeviceSignatureService deviceSignatureService;
    private KeyPair deviceKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        deviceSignatureService =
                new Ed25519DeviceSignatureServiceImpl();

        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("Ed25519");

        deviceKeyPair = keyPairGenerator.generateKeyPair();
    }

    @Test
    void validSignature_shouldBeAccepted()
            throws Exception {

        String payload =
                "{\"batchId\":8,\"vehicleId\":1,"
                        + "\"gps\":\"11.9404,108.4583\","
                        + "\"temperature\":7.2}";

        String signatureBase64 = sign(
                payload,
                deviceKeyPair.getPrivate()
        );

        String publicKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        deviceKeyPair.getPublic().getEncoded()
                );

        boolean valid =
                deviceSignatureService.verifySignature(
                        payload,
                        signatureBase64,
                        publicKeyBase64
                );

        assertTrue(valid);
    }

    @Test
    void modifiedPayload_shouldBeRejected()
            throws Exception {

        String originalPayload =
                "{\"batchId\":8,\"temperature\":7.2}";

        String modifiedPayload =
                "{\"batchId\":8,\"temperature\":25.0}";

        String signatureBase64 = sign(
                originalPayload,
                deviceKeyPair.getPrivate()
        );

        String publicKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        deviceKeyPair.getPublic().getEncoded()
                );

        /*
         * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
         * Thay đổi dữ liệu cảm biến sau khi thiết bị ký
         * phải làm chữ ký mất hiệu lực.
         */
        boolean valid =
                deviceSignatureService.verifySignature(
                        modifiedPayload,
                        signatureBase64,
                        publicKeyBase64
                );

        assertFalse(valid);
    }

    @Test
    void signatureFromAnotherDevice_shouldBeRejected()
            throws Exception {

        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("Ed25519");

        KeyPair anotherDevice =
                keyPairGenerator.generateKeyPair();

        String payload =
                "{\"batchId\":8,\"temperature\":7.2}";

        String signatureFromAnotherDevice = sign(
                payload,
                anotherDevice.getPrivate()
        );

        String registeredPublicKey = Base64.getEncoder()
                .encodeToString(
                        deviceKeyPair.getPublic().getEncoded()
                );

        boolean valid =
                deviceSignatureService.verifySignature(
                        payload,
                        signatureFromAnotherDevice,
                        registeredPublicKey
                );

        assertFalse(valid);
    }

    @Test
    void malformedSignature_shouldBeRejected() {
        String publicKeyBase64 = Base64.getEncoder()
                .encodeToString(
                        deviceKeyPair.getPublic().getEncoded()
                );

        boolean valid =
                deviceSignatureService.verifySignature(
                        "{\"batchId\":8}",
                        "not-valid-base64",
                        publicKeyBase64
                );

        assertFalse(valid);
    }

    private String sign(
            String payload,
            PrivateKey privateKey
    ) throws Exception {

        Signature signer =
                Signature.getInstance("Ed25519");

        signer.initSign(privateKey);

        signer.update(
                payload.getBytes(StandardCharsets.UTF_8)
        );

        return Base64.getEncoder()
                .encodeToString(signer.sign());
    }
}