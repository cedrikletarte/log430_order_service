package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;

public interface PlaceOrderWithIdempotencyUseCase {
    
    /* Place an order with idempotency check */
    PlaceOrderResponse placeOrderWithIdempotency(
            PlaceOrderCommand command, 
            String idempotencyKey,
            String ipAddress, 
            String userAgent);
    
    /* Check if a request is duplicate based on idempotency key */
    boolean isDuplicateRequest(String idempotencyKey, Long userId);
    
    /* Get cached response for an idempotency key */
    PlaceOrderResponse getCachedResponse(String idempotencyKey, Long userId);
}
