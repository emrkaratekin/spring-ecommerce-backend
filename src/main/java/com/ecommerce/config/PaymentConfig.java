package com.ecommerce.config;

import com.stripe.StripeClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({StripeProperties.class, OrderProperties.class})
public class PaymentConfig {

    /**
     * A single, thread-safe Stripe client instance instead of the global static Stripe.apiKey.
     * Using an injected client keeps the key out of global state and makes the gateway easy to mock in tests.
     */
    @Bean
    public StripeClient stripeClient(StripeProperties stripeProperties) {
        return new StripeClient(stripeProperties.secretKey());
    }
}
