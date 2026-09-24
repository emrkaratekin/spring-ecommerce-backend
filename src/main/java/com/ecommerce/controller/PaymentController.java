package com.ecommerce.controller;

import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderNumber}/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Idempotent: calling it again for the same order returns the same payment intent,
     * so it responds with 200 OK instead of creating a new resource each time.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> startPayment(@AuthenticationPrincipal User user,
                                                        @PathVariable String orderNumber) {
        return ResponseEntity.ok(paymentService.startPayment(user.getId(), orderNumber));
    }

    @GetMapping
    public ResponseEntity<PaymentResponse> getPayment(@AuthenticationPrincipal User user,
                                                      @PathVariable String orderNumber) {
        return ResponseEntity.ok(paymentService.getPayment(user.getId(), orderNumber));
    }
}
