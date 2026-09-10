package com.service;

public interface DataEncryptionService {

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Merkle root chỉ bảo vệ tính toàn vẹn, không che giấu nội dung.
     *
     * AES-256-GCM bổ sung:
     * - Confidentiality: che giấu dữ liệu riêng tư.
     * - Integrity: phát hiện ciphertext bị sửa.
     * - Authentication tag: xác thực nội dung mã hóa.
     *
     * context sẽ được dùng làm AAD để ciphertext bị ràng buộc
     * với đúng batch và record, tránh tráo ciphertext giữa các record.
     */
    String encrypt(String plainJson, String context);

    /*
     * Chỉ giải mã thành công khi:
     * - Đúng khóa.
     * - Đúng context.
     * - Ciphertext và authentication tag không bị sửa.
     */
    String decrypt(String encryptedJson, String context);
}