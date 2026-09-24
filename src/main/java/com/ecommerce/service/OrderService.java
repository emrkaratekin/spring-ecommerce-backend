package com.ecommerce.service;

import com.ecommerce.dto.request.CreateOrderRequest;
import com.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderSummaryResponse;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    // Customer operations (always scoped to the authenticated user)

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    PageResponse<OrderSummaryResponse> getMyOrders(Long userId, Pageable pageable);

    OrderResponse getMyOrder(Long userId, String orderNumber);

    OrderResponse cancelMyOrder(Long userId, String orderNumber);

    // Admin operations

    PageResponse<OrderSummaryResponse> getAllOrders(OrderStatus status, Pageable pageable);

    OrderResponse getOrder(String orderNumber);

    OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request);

    // System operations

    /**
     * Cancels an unpaid order whose payment window has passed and releases its stock.
     * Does nothing if the order is no longer PENDING (e.g. it was paid in the meantime).
     */
    void expireOrder(String orderNumber);
}
