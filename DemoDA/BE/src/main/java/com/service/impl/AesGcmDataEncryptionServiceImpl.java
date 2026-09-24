package com.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.DataEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmDataEncryptionServiceImpl
        implements DataEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 128;
    private static final int ENCRYPTION_VERSION = 1;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;
    private final ObjectMapper objectMapper;

    public AesGcmDataEncryptionServiceImpl(
            @Value("${security.aes.key-base64}") String keyBase64,
            ObjectMapper objectMapper
    ) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "AES key must not be blank"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "AES key must be valid Base64",
                    e
            );
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "AES-256 key must contain exactly 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
        this.objectMapper = objectMapper;
    }

    @Override
    public String encrypt(String plainJson, String context) {
        validateInput(plainJson, context);

        try {
            /*
             * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
             * Mỗi lần mã hóa sử dụng IV ngẫu nhiên 12 byte.
             * Không được tái sử dụng IV với cùng một khóa AES-GCM.
             */
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            AUTH_TAG_LENGTH_BITS,
                            iv
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            /*
             * AAD không bị mã hóa nhưng được xác thực.
             * Ciphertext chỉ giải mã được khi đúng context
             * của batch và record.
             */
            cipher.updateAAD(
                    context.getBytes(StandardCharsets.UTF_8)
            );

            byte[] cipherBytes = cipher.doFinal(
                    plainJson.getBytes(StandardCharsets.UTF_8)
            );

            EncryptedEnvelope envelope =
                    new EncryptedEnvelope(
                            ENCRYPTION_VERSION,
                            ALGORITHM,
                            Base64.getEncoder().encodeToString(iv),
                            Base64.getEncoder().encodeToString(cipherBytes)
                    );

            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot encrypt private JSON",
                    e
            );
        }
    }

    @Override
    public String decrypt(String encryptedJson, String context) {
        validateInput(encryptedJson, context);

        try {
            EncryptedEnvelope envelope =
                    objectMapper.readValue(
                            encryptedJson,
                            EncryptedEnvelope.class
                    );

            if (envelope.version() != ENCRYPTION_VERSION) {
                throw new IllegalArgumentException(
                        "Unsupported encryption version: "
                                + envelope.version()
                );
            }

            if (!ALGORITHM.equals(envelope.algorithm())) {
                throw new IllegalArgumentException(
                        "Unsupported encryption algorithm"
                );
            }

            byte[] iv = Base64.getDecoder().decode(envelope.iv());
            byte[] cipherBytes =
                    Base64.getDecoder().decode(envelope.ciphertext());

            if (iv.length != IV_LENGTH_BYTES) {
                throw new IllegalArgumentException(
                        "AES-GCM IV must contain exactly 12 bytes"
                );
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            AUTH_TAG_LENGTH_BITS,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            cipher.updateAAD(
                    context.getBytes(StandardCharsets.UTF_8)
            );

            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(
                    plainBytes,
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            /*
             * Sai khóa, sai context hoặc ciphertext bị sửa
             * đều làm AES-GCM authentication thất bại.
             */
            throw new IllegalStateException(
                    "Cannot decrypt or authenticate private JSON",
                    e
            );
        }
    }

    private void validateInput(String data, String context) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException(
                    "Data must not be blank"
            );
        }

        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException(
                    "Encryption context must not be blank"
            );
        }
    }

    private record EncryptedEnvelope(
            int version,
            String algorithm,
            String iv,
            String ciphertext
    ) {
    }
}