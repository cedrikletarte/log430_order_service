package com.brokerx.order_service.application.port.out;

import java.math.BigDecimal;

/**
 * Port for publishing order events
 * This follows hexagonal architecture by defining the output port in the application layer
 */
public interface OrderEventPort {
    
    /**
     * Publish an order placed event
     * @param event The order placed event data
     */
    void publishOrderPlaced(OrderPlacedEventData event);
    
    /**
     * Publish an order cancelled event
     * @param event The order cancelled event data
     */
    void publishOrderCancelled(OrderCancelledEventData event);
    
    /**
     * DTO for order placed event
     * Contains walletId instead of userId for wallet service operations
     */
    record OrderPlacedEventData(
        Long orderId,
        Long walletId,
        String stockSymbol,
        String side,
        String type,
        BigDecimal price,
        Integer quantity
    ) {}
    
    /**
     * DTO for order cancelled event
     * Triggers compensation/refund in wallet service
     */
    record OrderCancelledEventData(
        Long orderId,
        Long walletId,
        String stockSymbol,
        String side,
        BigDecimal refundAmount,
        String reason
    ) {}
}
