package com.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the whole application context starts on a fresh database.
 * This also proves that the Flyway migrations produce a schema that matches the JPA entities
 * (Hibernate runs with ddl-auto=validate and would fail the startup otherwise).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class EcommerceApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
