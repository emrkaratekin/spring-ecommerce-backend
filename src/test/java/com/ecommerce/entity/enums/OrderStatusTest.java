package com.ecommerce.entity.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "PENDING, PAID",
            "PENDING, CANCELLED",
            "PAID, SHIPPED",
            "SHIPPED, DELIVERED"
    })
    void allowsValidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "PENDING, SHIPPED",
            "PENDING, DELIVERED",
            "PAID, CANCELLED",
            "PAID, PENDING",
            "SHIPPED, CANCELLED",
            "DELIVERED, PENDING",
            "CANCELLED, PAID"
    })
    void rejectsInvalidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void finalStatesCannotChange() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.DELIVERED.canTransitionTo(target)).isFalse();
            assertThat(OrderStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }
}
