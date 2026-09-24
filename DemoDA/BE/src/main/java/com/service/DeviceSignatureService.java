package com.service;

public interface DeviceSignatureService {

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Mỗi thiết bị IoT sở hữu một cặp khóa Ed25519.
     *
     * - Private key chỉ nằm trên thiết bị và dùng để ký.
     * - Public key lưu trong hệ thống để backend xác minh.
     *
     * Backend không tự tạo chữ ký thay thiết bị như cách hash payload cũ.
     */
    boolean verifySignature(
            String payload,
            String signatureBase64,
            String publicKeyBase64
    );
}