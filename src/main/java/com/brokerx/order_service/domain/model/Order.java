package com.brokerx.order_service.domain.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/* Domain model representing a stock order */
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
    private Instant createdAt;
}