package com.brokerx.order_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/* Event published to wallet_service when an order is executed */
public record OrderExecutedEvent(
    Long orderId,
    Long walletId,
    String side,
    String stockSymbol,
    Integer quantity,
    BigDecimal executionPrice,
    BigDecimal totalAmount
) {}
