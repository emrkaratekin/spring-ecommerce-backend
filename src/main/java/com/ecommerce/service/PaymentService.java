package com.ecommerce.service;

import com.ecommerce.dto.response.PaymentResponse;

public interface PaymentService {

    /**
     * Starts (or resumes) the payment of a PENDING order and returns the client secret for the frontend.
     * Calling it again for the same order returns the same payment intent (idempotent).
     */
    PaymentResponse startPayment(Long userId, String orderNumber);

    PaymentResponse getPayment(Long userId, String orderNumber);

    void handleWebhook(String payload, String signatureHeader);
}
