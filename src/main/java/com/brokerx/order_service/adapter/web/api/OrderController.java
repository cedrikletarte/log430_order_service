package com.brokerx.order_service.adapter.web.api;

import com.brokerx.order_service.adapter.web.dto.PlaceOrderRequest;
import com.brokerx.order_service.application.port.in.command.OrderResponse;
import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.application.port.in.useCase.OrderUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller to handle order-related endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderUseCase OrderUseCase;

    
    public OrderController(
            OrderUseCase OrderUseCase) {
        this.OrderUseCase = OrderUseCase;
    }

    /**
     * Endpoint for placing a new order
     * POST /api/order
     */
    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        // Extract client information
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String userId = authentication.getPrincipal().toString();
        Long userIdLong = Long.parseLong(userId);

        log.info("Received order request: user={}, symbol={}, side={}, type={}, qty={}, limitPrice={}, executedPrice={}", 
                userId, request.stockSymbol(), request.side(), request.type(), 
                request.quantity(), request.limitPrice(), request.executedPrice());

        // Convert to command
        PlaceOrderCommand command = PlaceOrderCommand.builder()
                .userId(userIdLong)
                .stockSymbol(request.stockSymbol())
                .side(request.side())
                .type(request.type())
                .quantity(request.quantity())
                .limitPrice(request.limitPrice())
                .executedPrice(request.executedPrice())
                .build();

        // Execute use case
        PlaceOrderResponse response = OrderUseCase.placeOrder(command, ipAddress, userAgent);

        // Return response with appropriate HTTP status
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Extracts the client's real IP address, taking into account proxies.
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
     * Endpoint for retrieving an order by its ID
     * GET /api/order/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return OrderUseCase.getOrderById(orderId)
                .map(order -> ResponseEntity.ok(order))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint for retrieving all orders of a user
     * GET /api/order/user/{userId}
     */
    @GetMapping("/user")
    public ResponseEntity<List<OrderResponse>> getUserOrders(Authentication authentication) {
        String userId = authentication.getPrincipal().toString();

        Long userIdLong = Long.parseLong(userId);
        List<OrderResponse> orders = OrderUseCase.getOrdersByUserId(userIdLong);
        
        return ResponseEntity.ok(orders);
    }

    /**
     * Endpoint for retrieving active orders of a user
     * GET /api/order/user/{userId}/active
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<OrderResponse>> getUserActiveOrders(@PathVariable Long userId) {
        List<OrderResponse> orders = OrderUseCase.getActiveOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}