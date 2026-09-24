package com.service.impl;

import com.service.DeviceSignatureService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class Ed25519DeviceSignatureServiceImpl
        implements DeviceSignatureService {

    private static final String ALGORITHM = "Ed25519";

    @Override
    public boolean verifySignature(
            String payload,
            String signatureBase64,
            String publicKeyBase64
    ) {
        if (payload == null || payload.isBlank()) {
            return false;
        }

        if (signatureBase64 == null
                || signatureBase64.isBlank()) {
            return false;
        }

        if (publicKeyBase64 == null
                || publicKeyBase64.isBlank()) {
            return false;
        }

        try {
            byte[] publicKeyBytes =
                    Base64.getDecoder().decode(publicKeyBase64);

            byte[] signatureBytes =
                    Base64.getDecoder().decode(signatureBase64);

            KeyFactory keyFactory =
                    KeyFactory.getInstance(ALGORITHM);

            PublicKey publicKey = keyFactory.generatePublic(
                    new X509EncodedKeySpec(publicKeyBytes)
            );

            Signature verifier =
                    Signature.getInstance(ALGORITHM);

            verifier.initVerify(publicKey);

            verifier.update(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            /*
             * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
             * Backend chỉ giữ public key và chỉ xác minh.
             * Private key không xuất hiện trong backend hoặc database.
             *
             * Nếu payload hoặc signature bị thay đổi,
             * Ed25519 verification sẽ trả về false.
             */
            return verifier.verify(signatureBytes);

        } catch (Exception e) {
            /*
             * Public key sai định dạng, signature không hợp lệ
             * hoặc dữ liệu Base64 bị hỏng đều bị từ chối.
             */
            return false;
        }
    }
}