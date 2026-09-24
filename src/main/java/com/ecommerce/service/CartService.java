package com.ecommerce.service;

import com.ecommerce.dto.request.AddCartItemRequest;
import com.ecommerce.dto.request.UpdateCartItemRequest;
import com.ecommerce.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItemQuantity(Long userId, Long productId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}
