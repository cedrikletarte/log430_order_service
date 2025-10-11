package com.brokerx.order_service.domain.exception;

/**
 * Exception thrown when a duplicate idempotency key is detected
 */
public class DuplicateRequestException extends RuntimeException {
    
    public DuplicateRequestException(String message) {
        super(message);
    }
}
