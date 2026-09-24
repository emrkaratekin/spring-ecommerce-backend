package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must be at most 100 characters")
        String recipientName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Phone number must be a valid phone number")
        String phoneNumber,

        @NotBlank(message = "Street is required")
        @Size(max = 255, message = "Street must be at most 255 characters")
        String street,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must be at most 100 characters")
        String country
) {
}
