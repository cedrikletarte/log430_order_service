package com.brokerx.order_service.domain.service;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.domain.exception.OrderException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service de validation des ordres selon les règles métier
 * Implémente les contrôles pré-trade fondamentaux
 */
@Service
public class OrderValidator {
    
    /**
     * Valide un ordre avant soumission
     * @param order L'ordre à valider
     * @throws OrderException si l'ordre ne respecte pas les règles métier
     */
    public void validateForCreation(Order order) {
        validateBasicFields(order);
        validateQuantity(order);
        validatePrice(order);
        validateBusinessRules(order);
    }
    
    /**
     * Validation des champs obligatoires
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
        
        if (order.getUserId() == null) {
            throw new OrderException("MISSING_USER_ID", "L'identifiant utilisateur est obligatoire");
        }
    }
    
    /**
     * Validation de la quantité
     */
    private void validateQuantity(Order order) {
        BigDecimal quantity = order.getQuantity();
        
        if (quantity == null) {
            throw OrderException.invalidQuantity(null);
        }
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw OrderException.invalidQuantity(quantity);
        }
        
        // Vérification de base : la quantité doit être un entier positif
        if (quantity.stripTrailingZeros().scale() > 0) {
            throw new OrderException("INVALID_QUANTITY_DECIMAL", "La quantité doit être un nombre entier");
        }
    }
    
    /**
     * Validation du prix pour les ordres LIMIT
     */
    private void validatePrice(Order order) {
        if (order.getType() == OrderType.LIMIT) {
            BigDecimal price = order.getPrice();
            
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
            if (order.getPrice() != null) {
                throw new OrderException("MARKET_ORDER_WITH_PRICE", 
                    "Un ordre MARKET ne doit pas spécifier de prix");
            }
        }
    }
    
    /**
     * Validation des règles métier spécifiques
     */
    private void validateBusinessRules(Order order) {
        // Vérifier une limite simple de quantité
        BigDecimal maxQuantity = new BigDecimal("1000000");
        if (order.getQuantity().compareTo(maxQuantity) > 0) {
            throw new OrderException("ORDER_SIZE_TOO_LARGE", 
                String.format("La quantité %s dépasse la limite maximale %s", 
                    order.getQuantity(), maxQuantity));
        }
    }
}