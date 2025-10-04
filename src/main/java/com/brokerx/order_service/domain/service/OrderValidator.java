package com.brokerx.order_service.domain.service;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.domain.exception.OrderException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Validates orders based on business rules
 * Implements fundamental pre-trade checks
 */
@Service
public class OrderValidator {
    
    /**
     * Validates an order before submission
     */
    public void validateForCreation(Order order) {
        validateBasicFields(order);
        validateQuantity(order);
        validatePrice(order);
        validateBusinessRules(order);
    }
    
    /**
     * Validation of mandatory fields
     */
    private void validateBasicFields(Order order) {
        if (order.getStockId() == null) {
            throw new OrderException("MISSING_STOCK_ID", "L'identifiant du stock est obligatoire");
        }
        
        if (order.getSide() == null) {
            throw new OrderException("MISSING_SIDE", "Le sens de l'ordre (BUY/SELL) est obligatoire");
        }
        
        if (order.getType() == null) {
            throw new OrderException("MISSING_TYPE", "Le type d'ordre (MARKET/LIMIT) est obligatoire");
        }
        
        if (order.getWalletId() == null) {
            throw new OrderException("MISSING_WALLET_ID", "L'identifiant du portefeuille est obligatoire");
        }
    }
    
    /**
     * Validation of quantity
     */
    private void validateQuantity(Order order) {
        int quantity = order.getQuantity();
        
        if (quantity <= 0) {
            throw OrderException.invalidQuantity(quantity);
        }
        
        // Vérification de base : la quantité doit être un entier positif
        if (quantity > 0) {
            throw new OrderException("INVALID_QUANTITY_DECIMAL", "La quantité doit être un nombre entier");
        }
    }
    
    /**
     * Validation of price for LIMIT orders
     */
    private void validatePrice(Order order) {
        if (order.getType() == OrderType.LIMIT) {
            BigDecimal price = order.getLimitPrice();
            
            if (price == null) {
                throw new OrderException("MISSING_PRICE", "Le prix est obligatoire pour un ordre LIMIT");
            }
            
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderException("INVALID_PRICE", "Le prix doit être positif");
            }
            
            // Vérifier que le prix a au maximum 2 décimales
            if (price.scale() > 2) {
                throw new OrderException("INVALID_PRICE_PRECISION", "Le prix ne peut avoir plus de 2 décimales");
            }
        } else if (order.getType() == OrderType.MARKET) {
            // Les ordres MARKET ne doivent pas avoir de prix
            if (order.getExecutedPrice() != null) {
                throw new OrderException("MARKET_ORDER_WITH_PRICE", 
                    "Un ordre MARKET ne doit pas spécifier de prix");
            }
        }
    }
    
    /**
     * Validation of complex business rules
     */
    private void validateBusinessRules(Order order) {
        // Verify that the order quantity does not exceed a maximum limit
        int maxQuantity = 1000000;
        if (0 > order.getQuantity() || order.getQuantity() > maxQuantity) {
            throw new OrderException("ORDER_SIZE_TOO_LARGE", 
                String.format("La quantité %s dépasse la limite maximale %s", 
                    order.getQuantity(), maxQuantity));
        }
    }
}