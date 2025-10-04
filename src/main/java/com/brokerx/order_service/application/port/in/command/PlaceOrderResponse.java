package com.brokerx.order_service.application.port.in.command;

import com.brokerx.order_service.domain.model.OrderStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response returned after placing an order
 * Contains confirmation or rejection details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {
    private String orderId;
    private OrderStatus status;
    private String rejectReason;
    private Long stockId;
    private int quantity;
    private BigDecimal price;
    private BigDecimal reservedAmount;
    private LocalDateTime timestamp;
    private boolean success;
    
    public static PlaceOrderResponse accepted(String orderId, Long stockId,
                                            int quantity, BigDecimal price, BigDecimal reservedAmount) {
        return PlaceOrderResponse.builder()
                .orderId(orderId)
                .status(OrderStatus.ACCEPTED)
                .stockId(stockId)
                .quantity(quantity)
                .price(price)
                .reservedAmount(reservedAmount)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }
    
    public static PlaceOrderResponse rejected(Long stockId, String rejectReason) {
        return PlaceOrderResponse.builder()
                .status(OrderStatus.REJECTED)
                .stockId(stockId)
                .rejectReason(rejectReason)
                .timestamp(LocalDateTime.now())
                .success(false)
                .build();
    }
}