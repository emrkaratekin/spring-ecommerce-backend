package com.ecommerce.payment.stripe;

import com.ecommerce.config.StripeProperties;
import com.ecommerce.exception.InvalidWebhookSignatureException;
import com.ecommerce.exception.PaymentProviderException;
import com.ecommerce.payment.PaymentEvent;
import com.ecommerce.payment.PaymentGateway;
import com.ecommerce.payment.PaymentIntentResult;
import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    private static final String EVENT_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";

    private final StripeClient stripeClient;
    private final StripeProperties stripeProperties;

    @Override
    public PaymentIntentResult createPaymentIntent(String orderNumber, long amountInMinorUnits, String currency,
                                                   String idempotencyKey) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInMinorUnits)
                .setCurrency(currency)
                .setDescription("Order " + orderNumber)
                .putMetadata("orderNumber", orderNumber)
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build())
                .build();

        // The idempotency key guarantees that retries (network timeouts, double clicks)
        // never create a second payment intent for the same order.
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        try {
            return toResult(stripeClient.v1().paymentIntents().create(params, options));
        } catch (StripeException ex) {
            throw new PaymentProviderException("Could not create payment intent for order " + orderNumber, ex);
        }
    }

    @Override
    public PaymentIntentResult retrievePaymentIntent(String paymentIntentId) {
        try {
            return toResult(stripeClient.v1().paymentIntents().retrieve(paymentIntentId));
        } catch (StripeException ex) {
            throw new PaymentProviderException("Could not retrieve payment intent " + paymentIntentId, ex);
        }
    }

    @Override
    public void cancelPaymentIntent(String paymentIntentId) {
        try {
            stripeClient.v1().paymentIntents().cancel(paymentIntentId);
        } catch (StripeException ex) {
            throw new PaymentProviderException("Could not cancel payment intent " + paymentIntentId, ex);
        }
    }

    @Override
    public void refund(String paymentIntentId) {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("refund-" + paymentIntentId)
                .build();
        try {
            stripeClient.v1().refunds().create(params, options);
        } catch (StripeException ex) {
            throw new PaymentProviderException("Could not refund payment intent " + paymentIntentId, ex);
        }
    }

    @Override
    public PaymentEvent parseWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = stripeClient.constructEvent(payload, signatureHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException ex) {
            throw new InvalidWebhookSignatureException("Invalid Stripe webhook signature");
        } catch (RuntimeException ex) {
            throw new InvalidWebhookSignatureException("Malformed Stripe webhook payload");
        }

        PaymentEvent.Type type = switch (event.getType()) {
            case EVENT_PAYMENT_SUCCEEDED -> PaymentEvent.Type.PAYMENT_SUCCEEDED;
            case EVENT_PAYMENT_FAILED -> PaymentEvent.Type.PAYMENT_FAILED;
            default -> PaymentEvent.Type.IGNORED;
        };

        if (type == PaymentEvent.Type.IGNORED) {
            return PaymentEvent.ignored(event.getType());
        }

        PaymentIntent paymentIntent = (PaymentIntent) deserialize(event);
        String failureMessage = paymentIntent.getLastPaymentError() != null
                ? paymentIntent.getLastPaymentError().getMessage()
                : null;

        return new PaymentEvent(type, event.getType(), paymentIntent.getId(), failureMessage);
    }

    /**
     * If the event was sent with a different API version than the SDK was built for,
     * the safe deserializer returns empty; we then fall back to the unsafe (best-effort) one.
     */
    private StripeObject deserialize(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject().orElseGet(() -> {
            try {
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException ex) {
                throw new PaymentProviderException("Could not deserialize Stripe event " + event.getId(), ex);
            }
        });
    }

    private PaymentIntentResult toResult(PaymentIntent paymentIntent) {
        return new PaymentIntentResult(
                paymentIntent.getId(),
                paymentIntent.getClientSecret(),
                paymentIntent.getStatus()
        );
    }
}
