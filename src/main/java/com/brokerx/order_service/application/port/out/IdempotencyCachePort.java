package com.brokerx.order_service.application.port.out;

/**
 * Port for managing idempotency cache
 */
public interface IdempotencyCachePort {
    
    /**
     * Check if an idempotency key exists and is valid
     */
    boolean isDuplicate(String idempotencyKey, Long userId);
    
    /**
     * Get the cached response for an idempotency key
     */
    Object getCachedResponse(String idempotencyKey, Long userId);
    
    /**
     * Store the response for an idempotency key
     */
    void storeResponse(String idempotencyKey, Long userId, Object response);
}
