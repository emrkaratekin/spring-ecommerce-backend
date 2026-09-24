package com.ecommerce.dto.response;

import com.ecommerce.entity.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * clientSecret is only returned when a payment is started; the frontend passes it to Stripe.js
 * to confirm the payment. Card data never touches our server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        String orderNumber,
        String paymentIntentId,
        String clientSecret,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String failureReason
) {
}
