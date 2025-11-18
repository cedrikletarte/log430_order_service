package com.brokerx.order_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/* Event received from matching_service when orders are matched */
public record OrderMatchedEvent(
    Long buyOrderId,
    Long sellOrderId,
    String stockSymbol,
    Integer quantity,
    BigDecimal executionPrice
) {}
