package com.ecommerce.entity.enums;

/**
 * Order lifecycle:
 * <pre>
 * PENDING ──► PAID ──► SHIPPED ──► DELIVERED
 *    │
 *    └──► CANCELLED
 * </pre>
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == SHIPPED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
