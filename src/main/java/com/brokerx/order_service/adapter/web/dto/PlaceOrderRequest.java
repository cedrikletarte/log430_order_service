package com.brokerx.order_service.adapter.web.dto;

import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO Record pour les requêtes de création d'ordre (version simplifiée)
 */
public record PlaceOrderRequest(
        
    @NotNull(message = "L'ID du stock est obligatoire")
    @Positive(message = "L'ID du stock doit être positif")
    Long stockId,
    
    @NotNull(message = "Le sens de l'ordre (BUY/SELL) est obligatoire")
    OrderSide side,
    
    @NotNull(message = "Le type d'ordre (MARKET/LIMIT) est obligatoire")
    OrderType type,
    
    @NotNull(message = "La quantité est obligatoire")
    @Positive(message = "La quantité doit être positive")
    @DecimalMax(value = "999999999", message = "La quantité est trop importante")
    BigDecimal quantity,
    
    @Positive(message = "Le prix doit être positif")
    @DecimalMax(value = "999999999.99", message = "Le prix est trop élevé")
    BigDecimal price // Optionnel pour les ordres MARKET
) {}