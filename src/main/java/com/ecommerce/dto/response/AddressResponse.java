package com.ecommerce.dto.response;

public record AddressResponse(
        String recipientName,
        String phoneNumber,
        String street,
        String city,
        String postalCode,
        String country
) {
}
