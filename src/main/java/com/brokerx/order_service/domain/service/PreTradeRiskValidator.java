package com.brokerx.order_service.domain.service;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.domain.exception.OrderException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service responsable des contrôles pré-trade liés au pouvoir d'achat et à la gestion des risques
 */
@Service
public class PreTradeRiskValidator {
    
    /**
     * Valide le pouvoir d'achat pour un ordre d'achat
     * @param order L'ordre à valider
     * @param availableBalance Le solde disponible dans le wallet de l'utilisateur
     * @param currentPrice Le prix actuel du marché (pour les ordres MARKET)
     * @throws OrderException si les fonds sont insuffisants
     */
    public void validatePurchasingPower(Order order, BigDecimal availableBalance, BigDecimal currentPrice) {
        if (order.getSide() != OrderSide.BUY) {
            return; // Pas de contrôle de pouvoir d'achat pour les ventes
        }
        
        BigDecimal requiredAmount = calculateRequiredAmount(order, currentPrice);
        
        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw OrderException.insufficientFunds(order.getStockId(), requiredAmount, availableBalance);
        }
    }
    
    /**
     * Valide les bandes de prix (price bands) pour éviter les ordres aberrants
     * @param order L'ordre à valider
     * @param currentPrice Le prix actuel du marché
     * @param maxDeviationPercent Déviation maximale autorisée en pourcentage (ex: 10.0 pour ±10%)
     */
    public void validatePriceBands(Order order, BigDecimal currentPrice, BigDecimal maxDeviationPercent) {
        if (order.getType() != OrderType.LIMIT || order.getPrice() == null || currentPrice == null) {
            return; // Pas de contrôle pour les ordres MARKET ou sans prix de référence
        }
        
        BigDecimal orderPrice = order.getPrice();
        BigDecimal deviation = maxDeviationPercent.divide(new BigDecimal("100")); // Convertir en décimal
        
        BigDecimal minPrice = currentPrice.multiply(BigDecimal.ONE.subtract(deviation));
        BigDecimal maxPrice = currentPrice.multiply(BigDecimal.ONE.add(deviation));
        
        if (orderPrice.compareTo(minPrice) < 0 || orderPrice.compareTo(maxPrice) > 0) {
            throw OrderException.invalidPrice(order.getStockId(), orderPrice, minPrice, maxPrice);
        }
    }
    
    /**
     * Valide les limites par utilisateur (taille maximale, notionnel maximum)
     * @param order L'ordre à valider
     * @param userDailyVolume Volume déjà traité par l'utilisateur aujourd'hui
     * @param maxDailyVolume Volume maximum autorisé par jour pour cet utilisateur
     * @param maxSingleOrderNotional Notionnel maximum pour un ordre unique
     */
    public void validateUserLimits(Order order, BigDecimal userDailyVolume, 
                                 BigDecimal maxDailyVolume, BigDecimal maxSingleOrderNotional) {
        
        // Vérifier le notionnel de l'ordre unique
        BigDecimal orderNotional = calculateOrderNotional(order);
        if (orderNotional != null && orderNotional.compareTo(maxSingleOrderNotional) > 0) {
            throw new OrderException("ORDER_NOTIONAL_TOO_LARGE", 
                String.format("Notionnel de l'ordre trop important: %s. Maximum autorisé: %s", 
                    orderNotional, maxSingleOrderNotional));
        }
        
        // Vérifier les limites journalières
        BigDecimal projectedDailyVolume = userDailyVolume.add(orderNotional != null ? orderNotional : BigDecimal.ZERO);
        if (projectedDailyVolume.compareTo(maxDailyVolume) > 0) {
            throw new OrderException("DAILY_VOLUME_LIMIT_EXCEEDED", 
                String.format("Limite de volume journalier dépassée. Volume actuel: %s, Limite: %s", 
                    projectedDailyVolume, maxDailyVolume));
        }
    }
    
    
    /**
     * Calcule le montant requis pour un ordre d'achat
     */
    private BigDecimal calculateRequiredAmount(Order order, BigDecimal currentPrice) {
        BigDecimal quantity = order.getQuantity();
        
        if (order.getType() == OrderType.LIMIT) {
            return quantity.multiply(order.getPrice());
        } else if (order.getType() == OrderType.MARKET && currentPrice != null) {
            // Pour un ordre MARKET, on utilise le prix actuel avec une marge de sécurité
            BigDecimal safetyMargin = new BigDecimal("1.05"); // +5% de marge
            return quantity.multiply(currentPrice).multiply(safetyMargin);
        }
        
        throw new OrderException("CANNOT_CALCULATE_REQUIRED_AMOUNT", 
            "Impossible de calculer le montant requis pour cet ordre");
    }
    
    /**
     * Calcule la valeur notionnelle d'un ordre
     */
    private BigDecimal calculateOrderNotional(Order order) {
        if (order.getType() == OrderType.LIMIT && order.getPrice() != null) {
            return order.getQuantity().multiply(order.getPrice());
        }
        // Pour les ordres MARKET, on ne peut pas calculer le notionnel précis à l'avance
        return null;
    }
}