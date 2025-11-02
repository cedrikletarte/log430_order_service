package com.brokerx.order_service.infrastructure.kafka.consumer;

import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderExecutedEvent;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderMatchedEvent;
import com.brokerx.order_service.infrastructure.kafka.producer.OrderEventProducer;
import com.brokerx.order_service.infrastructure.websocket.OrderNotificationService;
import com.brokerx.order_service.infrastructure.websocket.dto.OrderNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Kafka consumer for handling OrderMatched events from matching_service
 * This is an inbound adapter in hexagonal architecture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMatchedEventConsumer {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderNotificationService notificationService;

    @KafkaListener(
        topics = "${kafka.topic.order-matched:order.matched}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void handleOrderMatched(OrderMatchedEvent event) {
        log.info("📥 Received OrderMatched event: buyOrderId={}, sellOrderId={}, symbol={}, qty={} @ {}",
                event.buyOrderId(), event.sellOrderId(), event.stockSymbol(), 
                event.quantity(), event.executionPrice());

        try {
            // Update both buy and sell orders
            processMatchedOrder(event.buyOrderId(), event.quantity(), event.executionPrice(), "BUY", event.stockSymbol());
            processMatchedOrder(event.sellOrderId(), event.quantity(), event.executionPrice(), "SELL", event.stockSymbol());

            log.info("✅ Successfully processed OrderMatched event for orders {} and {}",
                    event.buyOrderId(), event.sellOrderId());
        } catch (Exception e) {
            log.error("❌ Failed to process OrderMatched event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process OrderMatched event", e);
        }
    }

    private void processMatchedOrder(Long orderId, Integer matchedQuantity, BigDecimal executionPrice, 
                                     String side, String stockSymbol) {
        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Update order with execution details
        order.setExecutedPrice(executionPrice);
        order.setStatus(OrderStatus.FILLED);

        // Save updated order
        orderRepository.save(order);

        // Calculate total amount for wallet transaction
        BigDecimal totalAmount = executionPrice.multiply(BigDecimal.valueOf(matchedQuantity));

        // Publish OrderExecuted event to wallet_service for settlement
        OrderExecutedEvent executedEvent = new OrderExecutedEvent(
                orderId,
                order.getWalletId(),
                side,
                stockSymbol,
                matchedQuantity,
                executionPrice,
                totalAmount
        );

        orderEventProducer.publishOrderExecuted(executedEvent);

        // Send WebSocket notification to user
        sendOrderExecutionNotification(order, matchedQuantity, executionPrice, totalAmount, stockSymbol);

        log.info("📊 Order {} updated: status=FILLED, executedPrice={}, totalAmount={}",
                orderId, executionPrice, totalAmount);
    }

    /**
     * Send WebSocket notification to user about order execution
     * Uses walletId as the user identifier since each wallet belongs to one user
     * In this system, walletId == userId (1-to-1 relationship)
     */
    private void sendOrderExecutionNotification(Order order, Integer quantity, 
                                                BigDecimal executionPrice, BigDecimal totalAmount,
                                                String stockSymbol) {
        try {
            // walletId == userId in this system (1-to-1 mapping)
            // The Gateway sends X-User-Id header which equals the walletId
            String userId = String.valueOf(order.getWalletId());

            OrderNotification notification = OrderNotification.filled(
                    order.getId(),
                    stockSymbol,
                    order.getSide().toString(),
                    order.getType().toString(),
                    quantity,
                    executionPrice,
                    totalAmount,
                    java.time.Instant.now()
            );

            notificationService.notifyOrderExecution(userId, notification);
            
            log.info("📨 Sent WebSocket notification for user {} (order {})", userId, order.getId());
            
        } catch (Exception e) {
            log.error("Failed to send notification for order {}: {}", order.getId(), e.getMessage());
            // Don't fail the transaction if notification fails
        }
    }
}
