package com.brokerx.order_service.domain.exception;

public class DuplicateRequestException extends RuntimeException {
    
    public DuplicateRequestException(String message) {
        super(message);
    }
}
