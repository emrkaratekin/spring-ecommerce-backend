package com.ecommerce.service.impl;

import com.ecommerce.dto.request.AddCartItemRequest;
import com.ecommerce.dto.request.UpdateCartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 99;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(Long userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Product product = findActiveProductOrThrow(request.productId());
        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = findItem(cart, product.getId());
        int newQuantity = existingItem.map(CartItem::getQuantity).orElse(0) + request.quantity();
        validateQuantity(product, newQuantity);

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(newQuantity);
        } else {
            cart.addItem(CartItem.builder()
                    .product(product)
                    .quantity(newQuantity)
                    .build());
        }

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = findCartOrThrow(userId);
        CartItem item = findItemOrThrow(cart, productId);

        Product product = item.getProduct();
        if (!product.isActive()) {
            throw new BusinessException("Product '" + product.getName() + "' is no longer available");
        }
        validateQuantity(product, request.quantity());

        item.setQuantity(request.quantity());
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = findCartOrThrow(userId);
        CartItem item = findItemOrThrow(cart, productId);

        cart.removeItem(item);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findWithItemsByUserId(userId).ifPresent(Cart::clearItems);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .user(userRepository.getReferenceById(userId))
                        .build()));
    }

    private Cart findCartOrThrow(Long userId) {
        return cartRepository.findWithItemsByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));
    }

    private Optional<CartItem> findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private CartItem findItemOrThrow(Cart cart, Long productId) {
        return findItem(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "productId", productId));
    }

    private Product findActiveProductOrThrow(Long productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private void validateQuantity(Product product, int quantity) {
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BusinessException("You cannot add more than " + MAX_QUANTITY_PER_ITEM
                    + " units of the same product to the cart");
        }
        if (quantity > product.getStockQuantity()) {
            throw new BusinessException("Only " + product.getStockQuantity() + " units of '"
                    + product.getName() + "' are available in stock");
        }
    }
}
