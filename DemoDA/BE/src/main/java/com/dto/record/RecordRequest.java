package com.dto.record;

import com.enums.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RecordRequest {

    @NotNull
    private RecordType recordType;

    @NotBlank
    private String rawJson;

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Phân biệt record công khai và record riêng tư.
     *
     * false: lưu JSON công khai trên IPFS.
     * true: mã hóa JSON bằng AES-256-GCM trước khi upload IPFS.
     *
     * Giá trị mặc định là false nên request cũ vẫn hoạt động.
     */
    private boolean privateData;

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public boolean isPrivateData() {
        return privateData;
    }

    public void setPrivateData(boolean privateData) {
        this.privateData = privateData;
    }
}