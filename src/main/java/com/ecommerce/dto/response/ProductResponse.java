package com.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
}