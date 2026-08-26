package com.dto.verify;

import com.enums.AnchorStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResponse {
    private boolean valid;
    private String message;

    private Long batchId;
    private String batchCode;
    private String batchName;

    private Long recordId;
    private String recordKey;

    private String merkleRoot;
    private String chainTxHash;
    private AnchorStatus anchorStatus;

    private String leafHash;
    private Integer leafIndex;

    private List<String> proof;
    private LocalDateTime verifiedAt;
}