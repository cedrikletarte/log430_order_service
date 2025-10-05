package com.brokerx.order_service.application.service;

import com.brokerx.order_service.application.port.in.command.OrderResponse;
import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.application.port.in.useCase.OrderUseCase;
import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.infrastructure.client.WalletServiceClient;
import com.brokerx.order_service.infrastructure.client.MarketServiceClient.StockResponse;
import com.brokerx.order_service.infrastructure.client.WalletServiceClient.WalletResponse;
import com.brokerx.order_service.infrastructure.client.MarketServiceClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderService implements OrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final WalletServiceClient walletServiceClient;
    private final MarketServiceClient marketServiceClient;

    public OrderService(OrderRepositoryPort orderRepositoryPort,
            WalletServiceClient walletServiceClient,
            MarketServiceClient marketServiceClient) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.walletServiceClient = walletServiceClient;
        this.marketServiceClient = marketServiceClient;
    }

    @Override
    public PlaceOrderResponse placeOrder(PlaceOrderCommand command, String ipAddress, String userAgent) {
        log.info("Placing order: user={}, stock={}, side={}, type={}, qty={}, ip={}",
                command.getUserId(), command.getStockSymbol(), command.getSide(),
                command.getType(), command.getQuantity(), ipAddress);

        // Validate the stock via the market service
        MarketServiceClient.StockResponse stockResponse = marketServiceClient
                .getStockBySymbol(command.getStockSymbol());
        if (stockResponse == null) {
            String reason = "Invalid stock symbol: " + command.getStockSymbol();
            log.warn("Order rejected: {}", reason);
            return PlaceOrderResponse.rejected(null, reason);
        }

        log.info("Stock response received: id={}, symbol={}, name={}, price={}",
                stockResponse.id(), stockResponse.symbol(), stockResponse.name(), stockResponse.currentPrice());

        // Retrieve the user's wallet
        WalletServiceClient.WalletResponse walletResponse = walletServiceClient.getWalletByUserId(command.getUserId());
        if (walletResponse == null) {
            String reason = "Wallet not found for user ID: " + command.getUserId();
            log.warn("Order rejected: {}", reason);
            return PlaceOrderResponse.rejected(null, reason);
        }

        // Calculate the reserved amount
        BigDecimal reservedAmount = calculateReservedAmount(command, stockResponse);

        // Check if wallet has sufficient balance
        if (walletResponse.balance().compareTo(reservedAmount) < 0) {
            String reason = String.format("Insufficient funds. Required: %s, Available: %s", 
                    reservedAmount, walletResponse.balance());
            log.warn("Order rejected: {}", reason);
            return PlaceOrderResponse.rejected(stockResponse.id(), reason);
        }

        log.info("Wallet balance check passed: required={}, available={}", 
                reservedAmount, walletResponse.balance());

        // Create the order
        Order order = Order.builder()
                .walletId(walletResponse.id())
                .stockId(stockResponse.id())
                .side(command.getSide())
                .type(command.getType())
                .quantity(command.getQuantity())
                .limitPrice(command.getLimitPrice())
                .executedPrice(command.getType() == OrderType.MARKET ? stockResponse.currentPrice() : null)
                .status(command.getType() == OrderType.MARKET ? OrderStatus.ACCEPTED : OrderStatus.FILLED)
                .build();

        // Save the order via the repository
        Order savedOrder = orderRepositoryPort.save(order);

        log.info("Order accepted: id={}, walletId={}, stockId={}",
                savedOrder.getId(), savedOrder.getWalletId(), savedOrder.getStockId());

        // If MARKET order and BUY side, execute the transaction immediately and debit the wallet
        if (command.getType() == OrderType.MARKET && command.getSide() == OrderSide.BUY) {
            log.info("Executing MARKET order: debiting {} from user {}", reservedAmount, command.getUserId());
            walletServiceClient.debitWallet(command.getUserId(), reservedAmount);
            log.info("MARKET order executed successfully for order ID: {}", savedOrder.getId());
        }
        // If MARKET order and SELL side, execute the transaction immediately and credit the wallet
        if (command.getType() == OrderType.MARKET && command.getSide() == OrderSide.SELL) {
            log.info("Executing MARKET order: crediting {} to user {}", reservedAmount, command.getUserId());
            walletServiceClient.creditWallet(command.getUserId(), reservedAmount);
            log.info("MARKET order executed successfully for order ID: {}", savedOrder.getId());
        }

        // Return the response
        return PlaceOrderResponse.accepted(
                savedOrder.getId().toString(),
                savedOrder.getStockId(),
                savedOrder.getQuantity(),
                savedOrder.getLimitPrice(),
                savedOrder.getExecutedPrice(),
                reservedAmount);
    }

    @Override
    public Optional<OrderResponse> getOrderById(String orderId) {
        try {
            Long id = Long.parseLong(orderId);
            Order order = orderRepositoryPort.findById(id).orElse(null);
            if (order == null) {
                return Optional.empty();
            }
            
            // Fetch the stock symbol
            MarketServiceClient.StockResponse stockResponse = marketServiceClient.getStockById(order.getStockId());
            String stockSymbol = stockResponse != null ? stockResponse.symbol() : "UNKNOWN";
            
            return Optional.of(OrderResponse.builder()
                    .id(order.getId())
                    .stockId(order.getStockId())
                    .stockSymbol(stockSymbol)
                    .side(order.getSide().toString())
                    .type(order.getType().toString())
                    .quantity(order.getQuantity())
                    .limitPrice(order.getLimitPrice())
                    .executedPrice(order.getExecutedPrice())
                    .status(order.getStatus().toString())
                    .build());
        } catch (NumberFormatException e) {
            log.warn("Invalid order ID format: {}", orderId);
            return Optional.empty();
        }
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        WalletResponse walletResponse = walletServiceClient.getWalletByUserId(userId);
        if (walletResponse == null) {
            log.warn("Wallet not found for user ID: {}", userId);
            return List.of();
        }

        List<OrderResponse> orders = new ArrayList<>();

        for (Order order : orderRepositoryPort.findByWalletId(walletResponse.id())) {
            // Fetch the stock symbol
            StockResponse stockResponse = marketServiceClient.getStockById(order.getStockId());
            String stockSymbol = stockResponse != null ? stockResponse.symbol() : "UNKNOWN";
            
            OrderResponse dto = OrderResponse.builder()
                    .id(order.getId())
                    .stockId(order.getStockId())
                    .stockSymbol(stockSymbol)
                    .side(order.getSide().toString())
                    .type(order.getType().toString())
                    .quantity(order.getQuantity())
                    .limitPrice(order.getLimitPrice())
                    .executedPrice(order.getExecutedPrice())
                    .status(order.getStatus().toString())
                    .build();
            orders.add(dto);
        }

        return orders;
    }

    @Override
    public List<OrderResponse> getActiveOrdersByUserId(Long userId) {
        log.warn("getActiveOrdersByUserId not yet implemented for user: {}", userId);
        return List.of();
    }

    /**
     * Calculates the amount to reserve in the user's wallet based on the order type and price.
     */
    private BigDecimal calculateReservedAmount(PlaceOrderCommand command,
            MarketServiceClient.StockResponse stockResponse) {
        BigDecimal price;

        // For a LIMIT order, use the specified limit price
        if (command.getLimitPrice() != null) {
            price = command.getLimitPrice();
        } else {
            // For a MARKET order, use the current market price
            price = stockResponse.currentPrice();
        }

        return price.multiply(BigDecimal.valueOf(command.getQuantity()));
    }
}
