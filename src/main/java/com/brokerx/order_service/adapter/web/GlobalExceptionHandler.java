package com.brokerx.order_service.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.brokerx.order_service.adapter.web.dto.ApiResponse;
import com.brokerx.order_service.domain.exception.OrderException;
import com.brokerx.order_service.domain.exception.DuplicateRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    /* Handles IllegalArgumentException by returning a JSON error response. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
            .badRequest()
            .body(new ApiResponse<>(
                "ERROR",
                "INVALID_ARGUMENT",
                ex.getMessage(),
                null
            ));
    }

    /* Handles OrderException by returning a JSON error response. */
    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderException(OrderException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(
                "ERROR",
                "ORDER_ERROR",
                ex.getMessage(),
                null
            ));
    }

    /* Handles DuplicateRequestException by returning a JSON error response. */
    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateRequestException(DuplicateRequestException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiResponse<>(
                "ERROR",
                "DUPLICATE_REQUEST",
                ex.getMessage(),
                null
            ));
    }

    /* Catches any other unexpected exceptions. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(
                "ERROR",
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                null
            ));
    }
}
