package com.dto.user.retailer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateRetailUnitPriceRequest(
        @NotNull(message = "Vui lòng nhập giá")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "Giá phải lớn hơn 0")
        BigDecimal price
) {}