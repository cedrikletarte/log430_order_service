package com.brokerx.order_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/**
 * Event published when a LIMIT order is accepted and ready for matching
 */
public record OrderAcceptedEvent(
    Long orderId,
    String stockSymbol,
    String side,  // BUY or SELL
    BigDecimal limitPrice,
    Integer quantity
) {}
