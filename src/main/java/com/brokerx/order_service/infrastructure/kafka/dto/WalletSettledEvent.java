package com.brokerx.order_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/* Event received from wallet_service when an order has been settled */
public record WalletSettledEvent(
    Long orderId,
    Long walletId,
    String side,
    String stockSymbol,
    Integer quantity,
    BigDecimal executionPrice,
    BigDecimal totalAmount
) {}
