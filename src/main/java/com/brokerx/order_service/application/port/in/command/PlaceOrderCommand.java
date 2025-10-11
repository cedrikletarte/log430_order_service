package com.brokerx.order_service.application.port.in.command;

import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;

import java.math.BigDecimal;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Command for placing a new order
 * Represents the input data provided by the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderCommand {
    private Long userId;
    private String stockSymbol;
    private OrderSide side;
    private OrderType type;
    private int quantity;
    private BigDecimal limitPrice;  // For LIMIT orders
}