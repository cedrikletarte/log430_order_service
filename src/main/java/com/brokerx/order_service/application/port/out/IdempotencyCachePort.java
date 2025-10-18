package com.brokerx.order_service.application.port.out;

/**
 * Port for managing idempotency cache
 * This is an outbound port in hexagonal architecture
 */
public interface IdempotencyCachePort {
    
    /**
     * Check if an idempotency key exists and is valid
     * 
     * @param idempotencyKey the unique idempotency key
     * @param userId the user identifier
     * @return true if duplicate, false otherwise
     */
    boolean isDuplicate(String idempotencyKey, Long userId);
    
    /**
     * Get the cached response for an idempotency key
     * 
     * @param idempotencyKey the unique idempotency key
     * @param userId the user identifier
     * @return the cached response object, or null if not found
     */
    Object getCachedResponse(String idempotencyKey, Long userId);
    
    /**
     * Store the response for an idempotency key
     * 
     * @param idempotencyKey the unique idempotency key
     * @param userId the user identifier
     * @param response the response to cache
     */
    void storeResponse(String idempotencyKey, Long userId, Object response);
}
