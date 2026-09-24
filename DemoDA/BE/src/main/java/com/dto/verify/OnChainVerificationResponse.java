package com.dto.verify;

import com.enums.RecordStage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OnChainVerificationResponse {

    private Long batchId;

    private String recordKey;

    private RecordStage recordStage;

    private String leafHash;

    private Integer stageLeafIndex;

    private List<String> proof;

    private String databaseStageRoot;

    private String onChainStageRoot;

    /*
     * Dữ liệu gốc có tạo đúng leafHash hay không.
     */
    private boolean dataValid;

    /*
     * Proof có khớp root trong PostgreSQL hay không.
     */
    private boolean localProofValid;

    /*
     * Root PostgreSQL có trùng root blockchain hay không.
     */
    private boolean rootMatchesBlockchain;

    /*
     * Smart contract xác minh proof thành công hay không.
     */
    private boolean contractProofValid;

    /*
     * Chỉ true khi toàn bộ các lớp xác minh đều hợp lệ.
     */
    private boolean valid;

    private String message;

    private LocalDateTime verifiedAt;
}