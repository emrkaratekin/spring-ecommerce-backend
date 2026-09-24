package com.ecommerce.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates public, non-sequential order numbers such as ORD-20260925-7KQ4M2XA.
 * Ambiguous characters (0/O, 1/I) are excluded so the number is easy to read over the phone.
 */
@Component
public class OrderNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_PART_LENGTH = 8;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder randomPart = new StringBuilder(RANDOM_PART_LENGTH);
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            randomPart.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "ORD-" + DATE_FORMAT.format(Instant.now()) + "-" + randomPart;
    }
}
