package com.dto.verify;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VerifyAllResponse {

    private Long batchId;
    private String batchCode;
    private String batchName;
    private boolean valid;
    private String message;
    private int totalRecords;
    private int validRecords;
    private int invalidRecords;
    private String anchorStatus;
    private List<VerifyResponse> results;
}