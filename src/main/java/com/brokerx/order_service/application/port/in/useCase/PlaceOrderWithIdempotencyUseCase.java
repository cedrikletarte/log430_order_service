package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;

/**
 * Use case for placing orders with idempotency support
 */
public interface PlaceOrderWithIdempotencyUseCase {
    
    /**
     * Place an order with idempotency check
     * 
     * @param command the order command
     * @param idempotencyKey the unique idempotency key
     * @param ipAddress the client IP address
     * @param userAgent the client user agent
     * @return the order response
     */
    PlaceOrderResponse placeOrderWithIdempotency(
            PlaceOrderCommand command, 
            String idempotencyKey,
            String ipAddress, 
            String userAgent);
    
    /**
     * Check if a request is duplicate based on idempotency key
     * 
     * @param idempotencyKey the unique idempotency key
     * @param userId the user identifier
     * @return true if duplicate, false otherwise
     */
    boolean isDuplicateRequest(String idempotencyKey, Long userId);
    
    /**
     * Get cached response for an idempotency key
     * 
     * @param idempotencyKey the unique idempotency key
     * @param userId the user identifier
     * @return the cached response, or null if not found
     */
    PlaceOrderResponse getCachedResponse(String idempotencyKey, Long userId);
}
