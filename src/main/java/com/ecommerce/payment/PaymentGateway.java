package com.ecommerce.payment;

/**
 * Abstraction over the external payment provider.
 * The business layer depends only on this interface, never on the Stripe SDK directly,
 * so the provider can be replaced (or mocked in tests) without touching the order/payment logic.
 */
public interface PaymentGateway {

    /**
     * @param idempotencyKey retries with the same key return the same intent instead of creating a new one
     */
    PaymentIntentResult createPaymentIntent(String orderNumber, long amountInMinorUnits, String currency,
                                            String idempotencyKey);

    PaymentIntentResult retrievePaymentIntent(String paymentIntentId);

    void cancelPaymentIntent(String paymentIntentId);

    void refund(String paymentIntentId);

    /**
     * Verifies the webhook signature and translates the provider event into our own domain event.
     */
    PaymentEvent parseWebhookEvent(String payload, String signatureHeader);
}
