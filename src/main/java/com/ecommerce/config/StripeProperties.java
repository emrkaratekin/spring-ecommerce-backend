package com.ecommerce.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Stripe credentials. The application refuses to start if they are missing,
 * which is better than failing on the first real payment.
 */
@Validated
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(

        @NotBlank(message = "stripe.secret-key is required (set STRIPE_SECRET_KEY)")
        String secretKey,

        @NotBlank(message = "stripe.webhook-secret is required (set STRIPE_WEBHOOK_SECRET)")
        String webhookSecret,

        @NotBlank
        @Pattern(regexp = "^[a-z]{3}$", message = "stripe.currency must be a lowercase ISO 4217 code, e.g. pln")
        String currency
) {
}
