package com.ecommerce.controller;

import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives events from Stripe. This endpoint is public (Stripe has no JWT),
 * so every request is authenticated by verifying the Stripe-Signature header instead.
 * The body is read as a raw String because the signature is computed over the exact bytes Stripe sent.
 */
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                              @RequestHeader("Stripe-Signature") String signatureHeader) {
        paymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok().build();
    }
}
