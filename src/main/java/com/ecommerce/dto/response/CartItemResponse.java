package com.ecommerce.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        String sku,
        String imageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        boolean available
) {
}
