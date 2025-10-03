package com.brokerx.order_service.application.port.in.command;

import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Commande pour créer un nouvel ordre
 * Représente les données d'entrée fournies par le client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderCommand {
    
    // Identifiants
    private Long userId; // Sera extrait du JWT en production
    
    // Caractéristiques de l'ordre
    private Long stockId;
    private OrderSide side;
    private OrderType type;
    
    // Quantité et prix
    private BigDecimal quantity;
    private BigDecimal price; // Obligatoire pour LIMIT, ignoré pour MARKET
}