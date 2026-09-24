package com.ecommerce.exception;

/**
 * A webhook request could not be verified as coming from the payment provider (mapped to 400 Bad Request).
 */
public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
