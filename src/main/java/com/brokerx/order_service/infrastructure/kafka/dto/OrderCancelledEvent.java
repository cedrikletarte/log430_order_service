package com.brokerx.order_service.infrastructure.kafka.dto;

/* Event published when an order is cancelled */
public record OrderCancelledEvent(
    Long orderId,
    String stockSymbol,
    String side
) {}
