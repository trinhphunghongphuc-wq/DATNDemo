package com.dto.user.retailer;

import java.math.BigDecimal;
import java.time.Instant;

public record RetailUnitPriceHistoryResponse(
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Instant changedAt
) {}