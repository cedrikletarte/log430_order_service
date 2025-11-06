package com.brokerx.order_service.adapter.web.api;

import com.brokerx.order_service.adapter.web.dto.CancelOrderResponse;
import com.brokerx.order_service.adapter.web.dto.ModifyOrderResponse;
import com.brokerx.order_service.adapter.web.dto.PlaceOrderRequest;
import com.brokerx.order_service.application.port.in.command.OrderResponse;
import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.application.port.in.useCase.CancelOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.GetOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.ModifyOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.PlaceOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.PlaceOrderWithIdempotencyUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.brokerx.order_service.application.port.in.command.CancelOrderCommand;
import com.brokerx.order_service.application.port.in.command.ModifyOrderCommand;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller to handle order-related endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    private final ModifyOrderUseCase modifyOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final PlaceOrderWithIdempotencyUseCase placeOrderWithIdempotencyUseCase;

    public OrderController(
            PlaceOrderUseCase placeOrderUseCase,
            ModifyOrderUseCase modifyOrderUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            PlaceOrderWithIdempotencyUseCase placeOrderWithIdempotencyUseCase) {
        this.modifyOrderUseCase = modifyOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.placeOrderWithIdempotencyUseCase = placeOrderWithIdempotencyUseCase;
    }

    /**
     * Endpoint to modify a LIMIT order if it is still PENDING or ACCEPTED
     * PATCH /api/v1/order/{orderId}
     */
    @PatchMapping("/{orderId}")
    public ResponseEntity<ModifyOrderResponse> modifyOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) Integer newQuantity,
            @RequestParam(required = false) BigDecimal newLimitPrice,
            Authentication authentication) {
        String userId = authentication.getPrincipal().toString();
        Long userIdLong = Long.parseLong(userId);

        // Retrieve the order to check business conditions
        Optional<OrderResponse> orderOpt = getOrderUseCase.getOrderById(orderId.toString());
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ModifyOrderResponse(false, "Order not found", orderId));
        }
        OrderResponse order = orderOpt.get();

        // Check that it is a LIMIT order and status is PENDING or ACCEPTED
        if (!OrderType.LIMIT.name().equals(order.getType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ModifyOrderResponse(false, "Only LIMIT orders can be modified", orderId));
        }
        if (!(OrderStatus.PENDING.name().equals(order.getStatus()) || OrderStatus.ACCEPTED.name().equals(order.getStatus()))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ModifyOrderResponse(false, "Order can only be modified if status is PENDING or ACCEPTED", orderId));
        }

        // Build the modify command
        ModifyOrderCommand cmd = ModifyOrderCommand.builder()
                .orderId(orderId)
                .userId(userIdLong)
                .newQuantity(newQuantity)
                .newLimitPrice(newLimitPrice != null ? newLimitPrice.intValue() : 0)
                .build();

        boolean success = modifyOrderUseCase.modifyOrder(cmd);
        if (success) {
            return ResponseEntity.ok(new ModifyOrderResponse(true, "Order modified successfully", orderId));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ModifyOrderResponse(false, "Order modification failed", orderId));
        }
    }

    /**
     * Endpoint to cancel an order if it belongs to the user
     * DELETE /api/v1/order/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        String userId = authentication.getPrincipal().toString();
        Long userIdLong = Long.parseLong(userId);

        // Build the cancel command
        CancelOrderCommand cmd = CancelOrderCommand.builder()
                        .orderId(orderId)
                        .userId(userIdLong)
                        .build();

        boolean success = cancelOrderUseCase.cancelOrder(cmd);
        if (success) {
            return ResponseEntity.ok(new CancelOrderResponse(true, "Order cancelled successfully", orderId));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CancelOrderResponse(false, "Order cancellation failed", orderId));
        }
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

        log.info("Received order request: user={}, idempotencyKey={}, symbol={}, side={}, type={}, qty={}, limitPrice={}", 
                userId, request.idempotencyKey(), request.stockSymbol(), request.side(), request.type(), 
                request.quantity(), request.limitPrice());

        // Convert to command
        PlaceOrderCommand command = PlaceOrderCommand.builder()
                .userId(userIdLong)
                .stockSymbol(request.stockSymbol())
                .side(request.side())
                .type(request.type())
                .quantity(request.quantity())
                .limitPrice(request.limitPrice())
                .build();

        // Execute use case with idempotency
        PlaceOrderResponse response = placeOrderWithIdempotencyUseCase.placeOrderWithIdempotency(
                command, 
                request.idempotencyKey(), 
                ipAddress, 
                userAgent);

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
     * Endpoint for retrieving all orders of a user
     * GET /api/order/user
     */
    @GetMapping("/user")
    public ResponseEntity<List<OrderResponse>> getUserOrders(Authentication authentication) {
        String userId = authentication.getPrincipal().toString();

        Long userIdLong = Long.parseLong(userId);
        List<OrderResponse> orders = getOrderUseCase.getOrdersByUserId(userIdLong);
        
        return ResponseEntity.ok(orders);
    }
}