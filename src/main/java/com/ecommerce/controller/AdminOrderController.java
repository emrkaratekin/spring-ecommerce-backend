package com.ecommerce.controller;

import com.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderSummaryResponse;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only endpoints. Access is restricted to ROLE_ADMIN in SecurityConfig (/api/v1/admin/**).
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(status, pageable));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrder(orderNumber));
    }

    @PatchMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable String orderNumber,
                                                           @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, request));
    }
}
