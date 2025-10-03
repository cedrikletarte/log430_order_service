package com.brokerx.order_service.application.port.in.command;

import com.brokerx.order_service.domain.model.OrderStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réponse retournée après placement d'un ordre
 * Contient les informations de confirmation ou de rejet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderResponse {
    
    // Identifiants
    private String orderId; // UUID généré par le système
    
    // État de l'ordre
    private OrderStatus status;
    private String rejectReason; // Présent uniquement si status = REJECTED
    
    // Informations de l'ordre accepté
    private Long stockId;
    private BigDecimal quantity;
    private BigDecimal price; // null pour les ordres MARKET
    private BigDecimal reservedAmount; // Montant réservé dans le wallet
    
    // Métadonnées
    private LocalDateTime timestamp;
    private boolean success;
    
    // Factory methods
    
    public static PlaceOrderResponse accepted(String orderId, Long stockId, 
                                            BigDecimal quantity, BigDecimal price, BigDecimal reservedAmount) {
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