package com.ecommerce.dto.response;

import com.ecommerce.entity.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        Instant createdAt
) {
}
