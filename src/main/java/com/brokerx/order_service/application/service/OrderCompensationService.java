package com.brokerx.order_service.application.service;

import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderFailedEvent;
import com.brokerx.order_service.infrastructure.kafka.producer.OrderEventProducer;
import com.brokerx.order_service.infrastructure.websocket.OrderNotificationService;
import com.brokerx.order_service.infrastructure.websocket.dto.OrderNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service responsible for compensating orders when matching fails
 * Part of Saga Choreography pattern - handles rollback logic
 * Follows hexagonal architecture - application service orchestrating domain and infrastructure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCompensationService {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderNotificationService notificationService;

    /**
     * Compensate a failed matching by:
     * 1. Change order status from ACCEPTED to REJECTED
     * 2. Publish ORDER_FAILED event to trigger wallet compensation
     * 3. Notify user via WebSocket
     */
    @Transactional
    public void compensateFailedMatching(
            Long orderId,
            String stockSymbol,
            String side,
            BigDecimal limitPrice,
            Integer quantity,
            String reason
    ) {
        log.info("🔄 Starting compensation for order {} - Reason: {}", orderId, reason);

        // 1. Find and update order status
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found for compensation: " + orderId));

        // Validate order is in ACCEPTED state (should be if saga is correct)
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            log.warn("⚠️ Order {} is in {} state, expected ACCEPTED - Compensation may be duplicate",
                    orderId, order.getStatus());
        }

        // Change status to REJECTED
        order.setStatus(OrderStatus.REJECTED);
        orderRepository.save(order);

        log.info("📝 Order {} status changed: ACCEPTED → REJECTED", orderId);

        // 2. Publish ORDER_FAILED event to wallet_service for funds restoration
        BigDecimal totalAmount = limitPrice.multiply(BigDecimal.valueOf(quantity));
        
        OrderFailedEvent failedEvent = new OrderFailedEvent(
                orderId,
                order.getWalletId(),
                side,
                stockSymbol,
                quantity,
                limitPrice,
                totalAmount,
                reason
        );

        orderEventProducer.publishOrderFailed(failedEvent);

        log.info("📤 Published ORDER_FAILED event to wallet_service for compensation - walletId={}, amount={}",
                order.getWalletId(), totalAmount);

        // 3. Notify user via WebSocket
        sendCompensationNotification(order, reason, stockSymbol);

        log.info("✅ Compensation completed for order {}", orderId);
    }

    /**
     * Send WebSocket notification to user about order rejection due to matching failure
     */
    private void sendCompensationNotification(Order order, String reason, String stockSymbol) {
        try {
            String userId = String.valueOf(order.getWalletId()); // walletId == userId
            
            OrderNotification notification = OrderNotification.rejected(
                    order.getId(),
                    stockSymbol,
                    order.getSide().name(),
                    order.getType().name(),
                    order.getQuantity(),
                    reason
            );

            notificationService.sendOrderNotificationToUser(userId, notification);
            
            log.debug("📨 WebSocket notification sent to user {} for order {}", userId, order.getId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order {}: {}", 
                    order.getId(), e.getMessage());
            // Don't throw - notification failure shouldn't break compensation
        }
    }
}
