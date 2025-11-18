    package com.brokerx.order_service.adapter.web.api;

    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.messaging.handler.annotation.MessageMapping;
    import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
    import org.springframework.messaging.simp.annotation.SubscribeMapping;
    import org.springframework.security.core.Authentication;
    import org.springframework.stereotype.Controller;

    /* WebSocket controller for handling order notifications */
    @Slf4j
    @Controller
    @RequiredArgsConstructor
    public class OrderWebSocketController {

        /* Handle subscription confirmation from clients */
        @MessageMapping("/orders/subscribe")
        public void subscribeToOrders(SimpMessageHeaderAccessor headerAccessor,
                                    Authentication authentication) {
            String sessionId = headerAccessor.getSessionId();
            String userId = authentication.getPrincipal().toString();
            
            log.info("Client {} subscribed to order notifications via session: {}", userId, sessionId);
        }

        /* Handles subscriptions to user-specific order queue */
        @SubscribeMapping("/user/queue/orders")
        public void subscribeToUserOrders(SimpMessageHeaderAccessor headerAccessor,
                                        Authentication authentication) {
            String sessionId = headerAccessor.getSessionId();
            String userId = authentication.getPrincipal().toString();
            
            log.info("Client {} subscribed to personal order queue via session: {}", userId, sessionId);
        }

        /* Handle unsubscribe requests */
        @MessageMapping("/orders/unsubscribe")
        public void unsubscribeFromOrders(SimpMessageHeaderAccessor headerAccessor,
                                        Authentication authentication) {
            String sessionId = headerAccessor.getSessionId();
            String userId = authentication.getPrincipal().toString();
            
            log.info("Client {} unsubscribed from order notifications (session: {})",
                    userId, sessionId);
        }
    }
