package com.ecommerce.payment;

/**
 * Provider-independent view of a payment intent.
 *
 * @param id           provider ID of the intent (e.g. pi_3Nk...)
 * @param clientSecret secret the frontend uses to confirm the payment with the provider's SDK
 * @param status       provider status (e.g. requires_payment_method, succeeded, canceled)
 */
public record PaymentIntentResult(
        String id,
        String clientSecret,
        String status
) {

    public boolean isCanceled() {
        return "canceled".equals(status);
    }
}
