package com.ecommerce.dto.response;

import com.ecommerce.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        AddressResponse shippingAddress,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
