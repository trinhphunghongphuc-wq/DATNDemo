package com.dto.user.producer;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProducerBatchResponse {

    private Long id;
    private String name;
    private String batchCode;

    // URL được dùng để tạo QR cho Consumer quét
    private String qrContent;

    private String merkleRoot;
    private String chainTxHash;

    private LocalDate expiryDate;

    private String status;
    private String anchorStatus;

    private LocalDateTime createdAt;

    private int recordCount;
}