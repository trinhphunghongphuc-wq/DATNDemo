package com.dto.user.distributor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnReceiptRequest {

    @NotBlank(message = "Vui lòng nhập tên kho nhận hàng")
    private String warehouseName;

    @NotBlank(message = "Vui lòng nhập địa chỉ kho")
    private String warehouseAddress;
}