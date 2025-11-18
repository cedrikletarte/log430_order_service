package com.brokerx.order_service.infrastructure.kafka.consumer;

import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.infrastructure.kafka.dto.WalletSettledEvent;
import com.brokerx.order_service.infrastructure.websocket.OrderNotificationService;
import com.brokerx.order_service.infrastructure.websocket.dto.OrderNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for handling WalletSettled events from wallet_service
 * Part of the Saga Choreography pattern
 * Sends WebSocket notifications AFTER the wallet has been updated
 * This ensures users see correct balances when they refresh after notification
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSettledEventConsumer {

    private final OrderRepositoryPort orderRepository;
    private final OrderNotificationService notificationService;

    /* Listens for WalletSettled events and sends notifications */
    @KafkaListener(
        topics = "${kafka.topic.wallet-settled:wallet.settled}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleWalletSettled(WalletSettledEvent event) {
        log.info("📥 Received WalletSettled event: orderId={}, walletId={}, side={}, symbol={}, qty={} @ {}",
                event.orderId(), event.walletId(), event.side(), event.stockSymbol(),
                event.quantity(), event.executionPrice());

        try {
            // Find the order to get additional details
            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.orderId()));

            // Send WebSocket notification NOW that wallet is updated
            sendOrderExecutionNotification(order, event);
            
            log.info("✅ WebSocket notification sent for orderId {} (wallet updated)", event.orderId());
        } catch (Exception e) {
            log.error("❌ Failed to send notification for WalletSettled event orderId {}: {}",
                    event.orderId(), e.getMessage(), e);
            // Don't throw - notification failure shouldn't break the saga flow
        }
    }

    /**
     * Send WebSocket notification to user about order execution
     * walletId == userId in this system (1-to-1 relationship)
     */
    private void sendOrderExecutionNotification(Order order, WalletSettledEvent event) {
        // walletId == userId in this system (1-to-1 mapping)
        String userId = String.valueOf(event.walletId());

        OrderNotification notification = OrderNotification.filled(
                event.orderId(),
                event.stockSymbol(),
                event.side(),
                order.getType().toString(),
                event.quantity(),
                event.executionPrice(),
                event.totalAmount(),
                java.time.Instant.now()
        );

        notificationService.notifyOrderExecution(userId, notification);
        
        log.info("📨 Sent WebSocket notification for user {} (order {})", userId, event.orderId());
    }
}
