package com.ecommerce.payment;

/**
 * Provider-independent payment event received via webhook.
 */
public record PaymentEvent(
        Type type,
        String providerEventType,
        String paymentIntentId,
        String failureMessage
) {

    public enum Type {
        PAYMENT_SUCCEEDED,
        PAYMENT_FAILED,
        IGNORED
    }

    public static PaymentEvent ignored(String providerEventType) {
        return new PaymentEvent(Type.IGNORED, providerEventType, null, null);
    }
}
