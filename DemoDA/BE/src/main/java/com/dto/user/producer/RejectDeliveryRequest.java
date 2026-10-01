package com.dto.user.producer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RejectDeliveryRequest {

    @NotBlank(message = "Vui lòng nhập lý do trả hàng")
    private String reason;

    @NotNull(message = "Vui lòng nhập khối lượng trả")
    @Positive(message = "Khối lượng trả phải lớn hơn 0")
    private Double returnedWeight;

    @NotBlank(message = "Vui lòng cung cấp mã ảnh minh chứng")
    private String evidenceCid;
}