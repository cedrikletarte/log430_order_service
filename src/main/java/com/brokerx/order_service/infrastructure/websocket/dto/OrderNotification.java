package com.brokerx.order_service.infrastructure.websocket.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for order execution notifications sent via WebSocket
 */
public record OrderNotification(
    Long orderId,
    String stockSymbol,
    String side,
    String type,
    Integer quantity,
    BigDecimal executionPrice,
    BigDecimal totalAmount,
    String status,
    Instant executedAt,
    String message
) {
    /**
     * Create a notification for a filled order
     */
    public static OrderNotification filled(
            Long orderId,
            String stockSymbol,
            String side,
            String type,
            Integer quantity,
            BigDecimal executionPrice,
            BigDecimal totalAmount,
            Instant executedAt) {
        
        String message = String.format(
            "Your %s order for %d shares of %s has been filled at $%.2f (Total: $%.2f)",
            side, quantity, stockSymbol, executionPrice, totalAmount
        );
        
        return new OrderNotification(
            orderId,
            stockSymbol,
            side,
            type,
            quantity,
            executionPrice,
            totalAmount,
            "FILLED",
            executedAt,
            message
        );
    }

    /**
     * Create a notification for a partially filled order
     */
    public static OrderNotification partiallyFilled(
            Long orderId,
            String stockSymbol,
            String side,
            String type,
            Integer executedQuantity,
            Integer totalQuantity,
            BigDecimal executionPrice,
            BigDecimal totalAmount,
            Instant executedAt) {
        
        String message = String.format(
            "Your %s order for %s: %d/%d shares filled at $%.2f",
            side, stockSymbol, executedQuantity, totalQuantity, executionPrice
        );
        
        return new OrderNotification(
            orderId,
            stockSymbol,
            side,
            type,
            executedQuantity,
            executionPrice,
            totalAmount,
            "PARTIALLY_FILLED",
            executedAt,
            message
        );
    }

    /**
     * Create a notification for a rejected order
     */
    public static OrderNotification rejected(
            Long orderId,
            String stockSymbol,
            String side,
            String type,
            Integer quantity,
            String reason) {
        
        String message = String.format(
            "Your %s order for %d shares of %s was rejected: %s",
            side, quantity, stockSymbol, reason
        );
        
        return new OrderNotification(
            orderId,
            stockSymbol,
            side,
            type,
            quantity,
            null,
            null,
            "REJECTED",
            Instant.now(),
            message
        );
    }

    /**
     * Create a notification for a cancelled order
     */
    public static OrderNotification cancelled(
            Long orderId,
            String stockSymbol,
            String side,
            String type,
            Integer quantity) {
        
        String message = String.format(
            "Your %s order for %d shares of %s has been cancelled",
            side, quantity, stockSymbol
        );
        
        return new OrderNotification(
            orderId,
            stockSymbol,
            side,
            type,
            quantity,
            null,
            null,
            "CANCELLED",
            Instant.now(),
            message
        );
    }
}
