package com.brokerx.order_service.adapter.web;

import com.brokerx.order_service.adapter.web.dto.PlaceOrderRequest;
import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.application.port.in.useCase.OrderUseCase;
import com.brokerx.order_service.domain.model.Order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Contrôleur REST pour les opérations sur les ordres
 */
@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*") // À configurer selon les besoins de sécurité
public class OrderController {
    
    private final OrderUseCase OrderUseCase;
    
    public OrderController(OrderUseCase OrderUseCase) {
        this.OrderUseCase = OrderUseCase;
    }
    
    /**
     * Endpoint pour placer un nouvel ordre
     * POST /api/order
     */
    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        // Extraire les métadonnées de la requête HTTP
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String userId = authentication.getPrincipal().toString();
        
        // Convertir la requête en commande
        PlaceOrderCommand command = PlaceOrderCommand.builder()
                .userId(Long.parseLong(userId))
                .stockId(request.stockId())
                .side(request.side())
                .type(request.type())
                .quantity(request.quantity())
                .price(request.price())
                .build();
        
        // Exécuter le cas d'usage
        PlaceOrderResponse response = OrderUseCase.placeOrder(command, ipAddress, userAgent);
        
        // Retourner la réponse avec le statut HTTP approprié
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Extrait l'adresse IP réelle du client en tenant compte des proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Endpoint pour récupérer un ordre par son ID
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return OrderUseCase.getOrderById(orderId)
                .map(order -> ResponseEntity.ok(order))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Endpoint pour récupérer tous les ordres d'un utilisateur
     * GET /api/v1/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<java.util.List<Order>> getUserOrders(@PathVariable Long userId) {
        java.util.List<Order> orders = OrderUseCase.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Endpoint pour récupérer les ordres actifs d'un utilisateur
     * GET /api/v1/orders/user/{userId}/active
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<java.util.List<Order>> getUserActiveOrders(@PathVariable Long userId) {
        java.util.List<Order> orders = OrderUseCase.getActiveOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}