package com.ecommerce.service;

import com.ecommerce.config.OrderProperties;
import com.ecommerce.entity.enums.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Stock is reserved as soon as an order is created. If the customer never pays,
 * this job cancels the order after the payment timeout and releases the stock for other customers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderProperties orderProperties;

    @Scheduled(fixedDelayString = "${app.order.expiration-check-interval}")
    public void expireUnpaidOrders() {
        Instant cutoff = Instant.now().minus(orderProperties.paymentTimeout());
        List<String> expiredOrderNumbers =
                orderRepository.findOrderNumbersByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);

        for (String orderNumber : expiredOrderNumbers) {
            // Each order is expired in its own transaction, so one failure does not block the others.
            try {
                orderService.expireOrder(orderNumber);
                log.info("Order {} expired: not paid within {}", orderNumber, orderProperties.paymentTimeout());
            } catch (RuntimeException ex) {
                log.warn("Could not expire order {}: {}", orderNumber, ex.getMessage());
            }
        }
    }
}
