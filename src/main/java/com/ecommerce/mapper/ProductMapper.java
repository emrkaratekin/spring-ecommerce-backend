package com.ecommerce.mapper;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .sku(normalizeSku(request.sku()))
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .imageUrl(request.imageUrl())
                .category(category)
                .build();
    }

    public void updateEntity(Product product, ProductRequest request, Category category) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setSku(normalizeSku(request.sku()));
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}