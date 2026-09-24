package com.ecommerce.controller;

import com.ecommerce.dto.request.CreateOrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderSummaryResponse;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal User user,
                                                     @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(user.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderNumber}")
                .buildAndExpand(created.orderNumber())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(user.getId(), pageable));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getMyOrder(@AuthenticationPrincipal User user,
                                                    @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getMyOrder(user.getId(), orderNumber));
    }

    /**
     * Cancellation is modelled as a sub-resource (noun) instead of a verb like /cancel.
     */
    @PostMapping("/{orderNumber}/cancellation")
    public ResponseEntity<OrderResponse> cancelMyOrder(@AuthenticationPrincipal User user,
                                                       @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.cancelMyOrder(user.getId(), orderNumber));
    }
}
