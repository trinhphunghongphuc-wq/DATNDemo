package com.dto.dashboard;

public class RecordsByBatchResponse {

    private Long batchId;
    private String batchName;
    private long recordCount;

    public RecordsByBatchResponse() {
    }

    public RecordsByBatchResponse(Long batchId, String batchName, long recordCount) {
        this.batchId = batchId;
        this.batchName = batchName;
        this.recordCount = recordCount;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }
}