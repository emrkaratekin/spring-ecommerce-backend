package com.ecommerce.dto.request;

import com.ecommerce.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
