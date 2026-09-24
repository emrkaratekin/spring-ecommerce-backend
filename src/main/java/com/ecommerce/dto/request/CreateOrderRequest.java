package com.ecommerce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(

        @NotNull(message = "Shipping address is required")
        @Valid
        AddressRequest shippingAddress
) {
}
