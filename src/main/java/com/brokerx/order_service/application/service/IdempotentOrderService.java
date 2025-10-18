package com.brokerx.order_service.application.service;

import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.application.port.in.useCase.OrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.PlaceOrderWithIdempotencyUseCase;
import com.brokerx.order_service.application.port.out.IdempotencyCachePort;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementing idempotent order placement
 * This orchestrates the order placement with idempotency checks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentOrderService implements PlaceOrderWithIdempotencyUseCase {

    private final OrderUseCase orderUseCase;
    private final IdempotencyCachePort idempotencyCachePort;

    @Override
    public PlaceOrderResponse placeOrderWithIdempotency(
            PlaceOrderCommand command,
            String idempotencyKey,
            String ipAddress,
            String userAgent) {
        
        log.info("Processing order with idempotency: userId={}, idempotencyKey={}", 
                command.getUserId(), idempotencyKey);
        
        // Check for duplicate request
        if (idempotencyCachePort.isDuplicate(idempotencyKey, command.getUserId())) {
            log.warn("Duplicate order request detected: idempotencyKey={}, userId={}", 
                    idempotencyKey, command.getUserId());
            
            // Return cached response if available
            Object cachedResponse = idempotencyCachePort.getCachedResponse(idempotencyKey, command.getUserId());
            if (cachedResponse instanceof PlaceOrderResponse) {
                log.info("Returning cached response for idempotency key: {}", idempotencyKey);
                return (PlaceOrderResponse) cachedResponse;
            }
            
            // If no cached response, return rejection
            return PlaceOrderResponse.rejected(
                null, 
                "Duplicate request detected. Order with this idempotency key was already processed."
            );
        }
        
        // Place the order
        PlaceOrderResponse response = orderUseCase.placeOrder(command, ipAddress, userAgent);
        
        // Store the response in cache with the idempotency key
        idempotencyCachePort.storeResponse(idempotencyKey, command.getUserId(), response);
        
        return response;
    }

    @Override
    public boolean isDuplicateRequest(String idempotencyKey, Long userId) {
        return idempotencyCachePort.isDuplicate(idempotencyKey, userId);
    }

    @Override
    public PlaceOrderResponse getCachedResponse(String idempotencyKey, Long userId) {
        Object cachedResponse = idempotencyCachePort.getCachedResponse(idempotencyKey, userId);
        if (cachedResponse instanceof PlaceOrderResponse) {
            return (PlaceOrderResponse) cachedResponse;
        }
        return null;
    }
}
