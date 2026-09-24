package com.ecommerce.service.impl;

import com.ecommerce.config.StripeProperties;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.entity.enums.PaymentStatus;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.PaymentMapper;
import com.ecommerce.payment.PaymentEvent;
import com.ecommerce.payment.PaymentGateway;
import com.ecommerce.payment.PaymentIntentResult;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final StripeProperties stripeProperties;

    @Override
    @Transactional
    public PaymentResponse startPayment(Long userId, String orderNumber) {
        Order order = findUserOrderOrThrow(userId, orderNumber);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Order '" + orderNumber + "' cannot be paid in status " + order.getStatus());
        }

        Optional<Payment> existing = paymentRepository.findByOrderId(order.getId());
        String idempotencyKey = "payment-intent-" + orderNumber;

        // Reuse the existing intent (e.g. the user refreshed the checkout page or a previous card was declined).
        if (existing.isPresent() && existing.get().getStripePaymentIntentId() != null) {
            Payment payment = existing.get();
            PaymentIntentResult intent = paymentGateway.retrievePaymentIntent(payment.getStripePaymentIntentId());
            if (!intent.isCanceled()) {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setFailureReason(null);
                return paymentMapper.toResponse(payment, orderNumber, intent.clientSecret());
            }
            // The old intent is cancelled: a new one is needed. Reusing the same idempotency key
            // would make Stripe return the cancelled intent again, so the key is derived from the old intent.
            idempotencyKey = idempotencyKey + "-replaces-" + intent.id();
        }

        // Stripe expects the amount in the smallest currency unit: 125.30 PLN -> 12530 grosze.
        long amountInMinorUnits = order.getTotalAmount().movePointRight(2).longValueExact();
        PaymentIntentResult intent = paymentGateway.createPaymentIntent(
                orderNumber, amountInMinorUnits, stripeProperties.currency(), idempotencyKey);

        Payment payment = existing.orElseGet(() -> Payment.builder().order(order).build());
        payment.setStripePaymentIntentId(intent.id());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(stripeProperties.currency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        return paymentMapper.toResponse(payment, orderNumber, intent.clientSecret());
    }

    @Override
    public PaymentResponse getPayment(Long userId, String orderNumber) {
        Order order = findUserOrderOrThrow(userId, orderNumber);
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderNumber", orderNumber));
        return paymentMapper.toResponse(payment, orderNumber, null);
    }

    /**
     * Stripe is the source of truth for payments: an order becomes PAID only when Stripe tells us so
     * through a signed webhook, never because the client says "I paid".
     */
    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        PaymentEvent event = paymentGateway.parseWebhookEvent(payload, signatureHeader);

        switch (event.type()) {
            case PAYMENT_SUCCEEDED -> handlePaymentSucceeded(event);
            case PAYMENT_FAILED -> handlePaymentFailed(event);
            case IGNORED -> log.debug("Ignoring Stripe event {}", event.providerEventType());
        }
    }

    private void handlePaymentSucceeded(PaymentEvent event) {
        Optional<Payment> found = paymentRepository.findByStripePaymentIntentId(event.paymentIntentId());
        if (found.isEmpty()) {
            log.warn("Received success for unknown payment intent {}", event.paymentIntentId());
            return;
        }

        Payment payment = found.get();

        // Webhooks can be delivered more than once; processing the same event twice must be harmless.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment {} already processed, skipping duplicate event", event.paymentIntentId());
            return;
        }

        Order order = payment.getOrder();

        if (order.getStatus() == OrderStatus.PENDING) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setFailureReason(null);
            order.setStatus(OrderStatus.PAID);
            log.info("Order {} paid (payment intent {})", order.getOrderNumber(), event.paymentIntentId());
            return;
        }

        // The money arrived but the order was cancelled/expired in the meantime: give the money back.
        log.warn("Payment succeeded for order {} in status {}. Refunding automatically.",
                order.getOrderNumber(), order.getStatus());
        paymentGateway.refund(event.paymentIntentId());
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason("Order was " + order.getStatus() + " before the payment completed; refunded automatically");
    }

    private void handlePaymentFailed(PaymentEvent event) {
        paymentRepository.findByStripePaymentIntentId(event.paymentIntentId()).ifPresentOrElse(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(event.failureMessage());
                log.info("Payment failed for order {}: {}", payment.getOrder().getOrderNumber(), event.failureMessage());
            }
        }, () -> log.warn("Received failure for unknown payment intent {}", event.paymentIntentId()));
    }

    private Order findUserOrderOrThrow(Long userId, String orderNumber) {
        return orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
    }
}
