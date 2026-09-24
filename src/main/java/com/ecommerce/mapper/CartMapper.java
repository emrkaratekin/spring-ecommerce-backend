package com.ecommerce.mapper;

import com.ecommerce.dto.response.CartItemResponse;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        int totalItems = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new CartResponse(items, totalItems, totalAmount);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        boolean available = product.isActive() && product.getStockQuantity() >= item.getQuantity();

        return new CartItemResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getImageUrl(),
                product.getPrice(),
                item.getQuantity(),
                subtotal,
                available
        );
    }
}
