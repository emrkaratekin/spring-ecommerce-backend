package com.ecommerce.exception;

/**
 * The external payment provider could not be reached or rejected the request (mapped to 502 Bad Gateway).
 */
public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
