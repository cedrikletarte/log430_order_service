package com.brokerx.order_service.infrastructure.websocket;

import com.brokerx.order_service.infrastructure.websocket.dto.OrderNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending real-time order notifications to clients via WebSocket
 * Uses Spring's SimpMessagingTemplate to send messages to specific users
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send order notification to a specific user
     * The message will be sent to /user/{userId}/queue/orders
     * 
     * @param userId The user ID (from JWT subject - user ID as string)
     * @param notification The notification payload
     */
    public void sendOrderNotificationToUser(String userId, OrderNotification notification) {
        try {
            String destination = "/queue/orders";
            messagingTemplate.convertAndSendToUser(
                    userId,
                    destination,
                    notification
            );
            
            log.info("📤 Sent order notification to user {}: orderId={}, status={}",
                    userId, notification.orderId(), notification.status());
        } catch (Exception e) {
            log.error("❌ Failed to send notification to user {}: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast order notification to all connected clients
     * The message will be sent to /topic/orders
     * Useful for admin dashboard or market-wide notifications
     * 
     * @param notification The notification payload
     */
    public void broadcastOrderNotification(OrderNotification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/orders", notification);
            
            log.info("📡 Broadcasted order notification: orderId={}, status={}",
                    notification.orderId(), notification.status());
        } catch (Exception e) {
            log.error("❌ Failed to broadcast notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send order execution notification to a specific user
     * This is the main method used when an order is executed
     * 
     * @param userId The user ID who placed the order
     * @param notification The execution notification
     */
    public void notifyOrderExecution(String userId, OrderNotification notification) {
        sendOrderNotificationToUser(userId, notification);
        
        log.info("🔔 User {} notified of order execution: orderId={}, status={}",
                userId, notification.orderId(), notification.status());
    }
}
