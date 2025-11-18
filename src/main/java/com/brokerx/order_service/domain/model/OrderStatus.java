package com.brokerx.order_service.domain.model;

/* Enum representing the status of an order */
public enum OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    FILLED,
    CANCELLED
}