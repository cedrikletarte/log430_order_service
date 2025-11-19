package com.brokerx.order_service.infrastructure.kafka.producer;

import com.brokerx.order_service.infrastructure.kafka.dto.OrderAcceptedEvent;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderCancelledEvent;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderExecutedEvent;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/* Kafka producer for publishing order-related events */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.order-accepted:order.accepted}")
    private String orderAcceptedTopic;

    @Value("${kafka.topic.order-cancelled:order.cancelled}")
    private String orderCancelledTopic;

    @Value("${kafka.topic.order-executed:order.executed}")
    private String orderExecutedTopic;

    @Value("${kafka.topic.order-failed:order.failed}")
    private String orderFailedTopic;

    /* Publish an OrderAccepted event to matching_service */
    public void publishOrderAccepted(OrderAcceptedEvent event) {
        try {
            kafkaTemplate.send(orderAcceptedTopic, event.stockSymbol(), event);
            log.info("Published OrderAccepted event to topic {}: orderId={}, symbol={}, side={}, qty={} @ {}",
                    orderAcceptedTopic, event.orderId(), event.stockSymbol(), event.side(), 
                    event.quantity(), event.limitPrice());
        } catch (Exception e) {
            log.error("Failed to publish OrderAccepted event for orderId {}: {}", 
                    event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish OrderAccepted event", e);
        }
    }

    /* Publish an OrderCancelled event to matching_service */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        try {
            kafkaTemplate.send(orderCancelledTopic, event.stockSymbol(), event);
            log.info("Published OrderCancelled event to topic {}: orderId={}, symbol={}, side={}",
                    orderCancelledTopic, event.orderId(), event.stockSymbol(), event.side());
        } catch (Exception e) {
            log.error("Failed to publish OrderCancelled event for orderId {}: {}", 
                    event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish OrderCancelled event", e);
        }
    }

    /* Publish an OrderExecuted event to wallet_service */
    public void publishOrderExecuted(OrderExecutedEvent event) {
        try {
            kafkaTemplate.send(orderExecutedTopic, event.stockSymbol(), event);
            log.info("Published OrderExecuted event to topic {}: orderId={}, walletId={}, qty={} @ {}, total={}",
                    orderExecutedTopic, event.orderId(), event.walletId(), event.quantity(), 
                    event.executionPrice(), event.totalAmount());
        } catch (Exception e) {
            log.error("Failed to publish OrderExecuted event for orderId {}: {}", 
                    event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish OrderExecuted event", e);
        }
    }

    /* Publish an OrderFailed event to wallet_service for compensation */
    public void publishOrderFailed(OrderFailedEvent event) {
        try {
            kafkaTemplate.send(orderFailedTopic, event.stockSymbol(), event);
            log.info("Published OrderFailed event to topic {}: orderId={}, walletId={}, amount={}, reason={}",
                    orderFailedTopic, event.orderId(), event.walletId(), event.totalAmount(), event.reason());
        } catch (Exception e) {
            log.error("Failed to publish OrderFailed event for orderId {}: {}", 
                    event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish OrderFailed event", e);
        }
    }
}
