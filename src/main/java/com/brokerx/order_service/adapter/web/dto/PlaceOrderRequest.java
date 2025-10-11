package com.brokerx.order_service.adapter.web.dto;

import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO Record for placing an order
 */
public record PlaceOrderRequest(
        
    @NotNull(message = "The idempotency key is required")
    @NotBlank(message = "The idempotency key cannot be empty")
    String idempotencyKey,
    
    @NotNull(message = "The stock symbol is required")
    @NotBlank(message = "The stock symbol cannot be empty")
    String stockSymbol,
    
    @NotNull(message = "The order side (BUY/SELL) is required")
    OrderSide side,
    
    @NotNull(message = "The order type (MARKET/LIMIT) is required")
    OrderType type,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @DecimalMax(value = "999999999", message = "Quantity is too large")
    int quantity,
    
    // For LIMIT orders - the price limit set by the user
    @Positive(message = "Limit price must be positive when specified")
    @DecimalMax(value = "999999999.99", message = "Limit price is too large")
    BigDecimal limitPrice,
    
    // For MARKET orders - will be null on request, set by backend after execution
    @Positive(message = "Executed price must be positive when specified")
    @DecimalMax(value = "999999999.99", message = "Executed price is too large")
    BigDecimal executedPrice
) {}