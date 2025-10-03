package com.brokerx.order_service.domain.model;

/**
 * États possibles d'un ordre dans le système
 */
public enum OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    FILLED
}