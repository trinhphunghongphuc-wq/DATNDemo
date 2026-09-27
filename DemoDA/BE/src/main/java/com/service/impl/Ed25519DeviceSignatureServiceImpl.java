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

    @Override
    public boolean verifySignature(
            String payload,
            String signatureBase64,
            String publicKeyBase64
    ) {
        if (payload == null || payload.isBlank()
                || signatureBase64 == null || signatureBase64.isBlank()
                || publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            return false;
        }

        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));

            return verifier.verify(signatureBytes);
        } catch (Exception exception) {
            return false;
        }
    }
}