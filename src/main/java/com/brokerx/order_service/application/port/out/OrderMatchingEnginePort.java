package com.brokerx.order_service.application.port.out;

import com.brokerx.order_service.domain.model.Order;

/**
 * Port de sortie pour envoyer les ordres au moteur d'appariement
 */
public interface OrderMatchingEnginePort {
    
    /**
     * Envoie un ordre au moteur d'appariement interne
     * @param order L'ordre à traiter
     * @return true si l'ordre a été accepté par le moteur, false sinon
     */
    boolean submitOrder(Order order);
    
    /**
     * Annule un ordre dans le moteur d'appariement
     * @param orderId L'ID de l'ordre à annuler
     * @return true si l'annulation a été acceptée, false sinon
     */
    boolean cancelOrder(String orderId);
}