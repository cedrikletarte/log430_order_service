package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Port d'entrée pour consulter les ordres
 */
public interface OrderUseCase {
    
    /**
     * Récupère un ordre par son ID
     */
    Optional<Order> getOrderById(String orderId);
    
    /**
     * Récupère tous les ordres d'un utilisateur
     */
    List<Order> getOrdersByUserId(Long userId);
    
    /**
     * Récupère les ordres actifs d'un utilisateur
     */
    List<Order> getActiveOrdersByUserId(Long userId);

    /**
     * Place un nouvel ordre dans le système après validation des contrôles pré-trade
     * 
     * @param command Les détails de l'ordre à placer
     * @param ipAddress Adresse IP du client (pour audit)
     * @param userAgent User agent du client (pour audit)
     * @return Réponse contenant soit l'ACK d'acceptation soit le rejet avec raison
     */
    PlaceOrderResponse placeOrder(PlaceOrderCommand command, String ipAddress, String userAgent);
}