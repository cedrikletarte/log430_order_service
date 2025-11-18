package com.brokerx.order_service.application.port.in.command;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/* Response object for order details */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse{
    private Long id;
    private Long stockId;
    private String stockSymbol;
    private String side;
    private String type;
    private int quantity;
    private BigDecimal limitPrice;
    private BigDecimal executedPrice;
    private String status;
}
