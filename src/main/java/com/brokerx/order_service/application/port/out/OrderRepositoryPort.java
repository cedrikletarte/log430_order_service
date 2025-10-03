package com.brokerx.order_service.application.port.out;

import com.brokerx.order_service.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie pour la persistance des ordres
 */
public interface OrderRepositoryPort {
    
    /**
     * Sauvegarde un ordre
     */
    Order save(Order order);
    
    /**
     * Trouve un ordre par son ID système
     */
    Optional<Order> findById(Long id);
    
    /**
     * Trouve un ordre par son ID système UUID
     */
    Optional<Order> findByOrderId(String orderId);
    
    /**
     * Trouve un ordre par son ID client pour un utilisateur donné (idempotence)
     */
    Optional<Order> findByClientOrderIdAndUserId(String clientOrderId, Long userId);
    
    /**
     * Trouve tous les ordres d'un utilisateur
     */
    List<Order> findByUserId(Long userId);
    
    /**
     * Trouve les ordres actifs d'un utilisateur (PENDING, ACCEPTED, PARTIALLY_FILLED)
     */
    List<Order> findActiveOrdersByUserId(Long userId);
    
    /**
     * Calcule le volume journalier d'un utilisateur
     */
    java.math.BigDecimal calculateDailyVolumeByUserId(Long userId, java.time.LocalDate date);
}