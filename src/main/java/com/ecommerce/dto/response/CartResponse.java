package com.ecommerce.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount
) {

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO.setScale(2));
    }
}
