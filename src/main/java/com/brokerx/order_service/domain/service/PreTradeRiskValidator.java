package com.brokerx.order_service.domain.service;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.domain.exception.OrderException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for pre-trade risk validations
 */
@Service
public class PreTradeRiskValidator {
    
    /**
     * Validates purchasing power for a buy order
     */
    public void validatePurchasingPower(Order order, BigDecimal availableBalance, BigDecimal currentPrice) {
        if (order.getSide() != OrderSide.BUY) {
            return; // No purchasing power check for sell orders
        }
        
        BigDecimal requiredAmount = calculateRequiredAmount(order, currentPrice);
        
        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw OrderException.insufficientFunds(order.getStockId(), requiredAmount, availableBalance);
        }
    }
    
    /**
     * Validates price bands to prevent anomalous orders
     */
    public void validatePriceBands(Order order, BigDecimal currentPrice, BigDecimal maxDeviationPercent) {
        if (order.getType() != OrderType.LIMIT || order.getLimitPrice() == null || currentPrice == null) {
            return; // No check for MARKET orders or without reference price
        }
        
        BigDecimal orderPrice = order.getLimitPrice();
        BigDecimal deviation = maxDeviationPercent.divide(new BigDecimal("100")); // Convertir en décimal
        
        BigDecimal minPrice = currentPrice.multiply(BigDecimal.ONE.subtract(deviation));
        BigDecimal maxPrice = currentPrice.multiply(BigDecimal.ONE.add(deviation));
        
        if (orderPrice.compareTo(minPrice) < 0 || orderPrice.compareTo(maxPrice) > 0) {
            //throw OrderException.invalidPrice(order.getStockId(), orderPrice, minPrice, maxPrice);
        }
    }
    
    /**
     * Validates user limits (max size, max notional)
     */
    public void validateUserLimits(Order order, BigDecimal userDailyVolume, 
                                 BigDecimal maxDailyVolume, BigDecimal maxSingleOrderNotional) {
        
        // Verify single order notional
        BigDecimal orderNotional = calculateOrderNotional(order);
        if (orderNotional != null && orderNotional.compareTo(maxSingleOrderNotional) > 0) {
            throw new OrderException("ORDER_NOTIONAL_TOO_LARGE", 
                String.format("Notionnel de l'ordre trop important: %s. Maximum autorisé: %s", 
                    orderNotional, maxSingleOrderNotional));
        }
        
        // Vérify daily volume limit
        BigDecimal projectedDailyVolume = userDailyVolume.add(orderNotional != null ? orderNotional : BigDecimal.ZERO);
        if (projectedDailyVolume.compareTo(maxDailyVolume) > 0) {
            throw new OrderException("DAILY_VOLUME_LIMIT_EXCEEDED", 
                String.format("Limite de volume journalier dépassée. Volume actuel: %s, Limite: %s", 
                    projectedDailyVolume, maxDailyVolume));
        }
    }
    
    
    /**
     * Calculate the required amount to reserve for a BUY order
     */
    private BigDecimal calculateRequiredAmount(Order order, BigDecimal currentPrice) {
        int quantity = order.getQuantity();
        
        if (order.getType() == OrderType.LIMIT) {
            return BigDecimal.valueOf(quantity).multiply(order.getLimitPrice());
        } else if (order.getType() == OrderType.MARKET && currentPrice != null) {
            // For a MARKET order, add a safety margin to account for price fluctuations
            BigDecimal safetyMargin = new BigDecimal("1.05"); // +5% margin
            return BigDecimal.valueOf(quantity).multiply(currentPrice).multiply(safetyMargin);
        }
        
        throw new OrderException("CANNOT_CALCULATE_REQUIRED_AMOUNT", 
            "Impossible de calculer le montant requis pour cet ordre");
    }
    
    /**
     * Calculate the notional value of an order
     */
    private BigDecimal calculateOrderNotional(Order order) {
        if (order.getType() == OrderType.LIMIT && order.getLimitPrice() != null) {
            return BigDecimal.valueOf(order.getQuantity()).multiply(order.getLimitPrice());
        }
        // For MARKET orders, we cannot calculate the exact notional in advance
        return null;
    }
}