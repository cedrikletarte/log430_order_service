package com.brokerx.order_service.domain.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Entité représentant un ordre de bourse
 * Suit les principes DDD avec méthodes métier intégrées
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    // Identifiants
    private Long id;
    private Long userId;
    private Long stockId;
    
    // Caractéristiques de l'ordre
    private OrderSide side;
    private OrderType type;
    
    // Quantités et prix
    private BigDecimal quantity;
    private BigDecimal price;
    
    // État et métadonnées
    private OrderStatus status;
}