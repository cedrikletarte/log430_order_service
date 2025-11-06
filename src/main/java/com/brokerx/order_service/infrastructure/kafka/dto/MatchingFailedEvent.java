package com.brokerx.order_service.infrastructure.kafka.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published by matching_service when order matching fails and compensation is needed
 * This triggers rollback in order_service (ACCEPTED -> REJECTED) and wallet_service (restore funds)
 */
public record MatchingFailedEvent(
    Long orderId,
    String stockSymbol,
    String side,
    BigDecimal limitPrice,
    Integer quantity,
    String reason,
    Instant compensatedAt
) {
}
