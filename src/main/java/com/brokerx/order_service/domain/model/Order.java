package com.brokerx.order_service.domain.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Domain model representing a stock order
 * Follows DDD principles with embedded business methods
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private Long walletId;
    private Long stockId;
    private OrderSide side;
    private OrderType type;
    private int quantity;
    private BigDecimal limitPrice;
    private BigDecimal executedPrice;
    private OrderStatus status;
}