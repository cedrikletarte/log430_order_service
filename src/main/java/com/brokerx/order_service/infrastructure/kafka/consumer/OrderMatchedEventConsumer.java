package com.brokerx.order_service.infrastructure.kafka.consumer;

import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderExecutedEvent;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderMatchedEvent;
import com.brokerx.order_service.infrastructure.kafka.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kafka consumer for handling OrderMatched events from matching_service
 * Part of the Saga Choreography pattern
 * This is an inbound adapter in hexagonal architecture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMatchedEventConsumer {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventProducer orderEventProducer;

    /* Listens for OrderMatched events and processes them */
    @KafkaListener(
        topics = "${kafka.topic.order-matched:order.matched}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void handleOrderMatched(OrderMatchedEvent event) {
        log.info("Received OrderMatched event: buyOrderId={}, sellOrderId={}, symbol={}, qty={} @ {}",
                event.buyOrderId(), event.sellOrderId(), event.stockSymbol(), 
                event.quantity(), event.executionPrice());

        try {
            // Update both buy and sell orders
            Order buyOrder = processMatchedOrder(event.buyOrderId(), event.quantity(), event.executionPrice());
            Order sellOrder = processMatchedOrder(event.sellOrderId(), event.quantity(), event.executionPrice());

            // Calculate total amount for wallet transaction
            BigDecimal totalAmount = event.executionPrice().multiply(BigDecimal.valueOf(event.quantity()));

            // Publish single OrderExecuted event with both buyer and seller info
            OrderExecutedEvent executedEvent = new OrderExecutedEvent(
                    event.buyOrderId(),
                    event.sellOrderId(),
                    buyOrder.getWalletId(),
                    sellOrder.getWalletId(),
                    event.stockSymbol(),
                    event.quantity(),
                    event.executionPrice(),
                    totalAmount
            );

            orderEventProducer.publishOrderExecuted(executedEvent);

            log.info("Successfully processed OrderMatched event for orders {} and {} - Published single OrderExecuted event",
                    event.buyOrderId(), event.sellOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderMatched event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process OrderMatched event", e);
        }
    }

    private Order processMatchedOrder(Long orderId, Integer matchedQuantity, BigDecimal executionPrice) {
        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // Skip if order is already FILLED (prevents re-processing old matches)
        if (order.getStatus() == OrderStatus.FILLED) {
            log.warn("Order {} is already FILLED, skipping match processing", orderId);
            return order;
        }

        // Validate remaining quantity (prevent processing duplicate events)
        if (order.getRemainingQuantity() < matchedQuantity) {
            log.error("Order {} has remainingQuantity={} but match wants to fill {}. Possible duplicate event!",
                    orderId, order.getRemainingQuantity(), matchedQuantity);
            return order;
        }

        // Update executed quantity and remaining quantity
        int newExecutedQuantity = order.getExecutedQuantity() + matchedQuantity;
        int newRemainingQuantity = order.getRemainingQuantity() - matchedQuantity;
        
        order.setExecutedQuantity(newExecutedQuantity);
        order.setRemainingQuantity(newRemainingQuantity);
        
        // Calculate weighted average execution price if there were previous executions
        if (order.getExecutedPrice() != null && order.getExecutedQuantity() > matchedQuantity) {
            // Weighted average: (prevPrice * prevQty + newPrice * newQty) / totalQty
            int previousQuantity = order.getExecutedQuantity() - matchedQuantity;
            BigDecimal previousTotal = order.getExecutedPrice().multiply(BigDecimal.valueOf(previousQuantity));
            BigDecimal newTotal = executionPrice.multiply(BigDecimal.valueOf(matchedQuantity));
            BigDecimal averagePrice = previousTotal.add(newTotal)
                    .divide(BigDecimal.valueOf(order.getExecutedQuantity()), 2, RoundingMode.HALF_UP);
            order.setExecutedPrice(averagePrice);
        } else {
            order.setExecutedPrice(executionPrice);
        }
        
        // Update status based on remaining quantity
        if (newRemainingQuantity == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }

        // Save updated order
        orderRepository.save(order);

        log.info("Order {} updated: status={}, executedQty={}, remainingQty={}, executedPrice={}",
                orderId, order.getStatus(), newExecutedQuantity, newRemainingQuantity, executionPrice);
        
        return order;
    }
}
