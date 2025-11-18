package com.brokerx.order_service.infrastructure.kafka.consumer;

import com.brokerx.order_service.application.service.OrderCompensationService;
import com.brokerx.order_service.infrastructure.kafka.dto.MatchingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for handling MatchingFailed events from matching_service
 * Triggers compensation (rollback) when order matching fails
 * Part of Saga Choreography pattern for distributed transaction management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingFailedEventConsumer {

    private final OrderCompensationService compensationService;

    /* Listens for MatchingFailed events and triggers compensation */
    @KafkaListener(
        topics = "${kafka.topic.matching-failed:matching.failed}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void handleMatchingFailed(MatchingFailedEvent event) {
        log.warn("⚠️ Received MatchingFailed event: orderId={}, symbol={}, side={}, reason={}",
                event.orderId(), event.stockSymbol(), event.side(), event.reason());

        try {
            // Compensate the order by changing status from ACCEPTED to REJECTED
            compensationService.compensateFailedMatching(
                    event.orderId(),
                    event.stockSymbol(),
                    event.side(),
                    event.limitPrice(),
                    event.quantity(),
                    event.reason()
            );
            
            log.info("✅ Order compensation completed for orderId {} - Status changed to REJECTED", 
                    event.orderId());
        } catch (Exception e) {
            log.error("❌ Failed to compensate order {}: {}", 
                    event.orderId(), e.getMessage(), e);
            throw new RuntimeException("Failed to compensate order", e);
        }
    }
}
