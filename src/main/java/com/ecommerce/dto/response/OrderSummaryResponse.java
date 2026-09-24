package com.ecommerce.dto.response;

import com.ecommerce.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
