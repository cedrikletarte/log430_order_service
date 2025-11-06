package com.brokerx.order_service.application.service;

import com.brokerx.order_service.application.port.in.command.CancelOrderCommand;
import com.brokerx.order_service.application.port.in.command.ModifyOrderCommand;
import com.brokerx.order_service.application.port.in.command.OrderResponse;
import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;
import com.brokerx.order_service.application.port.in.useCase.CancelOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.GetOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.ModifyOrderUseCase;
import com.brokerx.order_service.application.port.in.useCase.PlaceOrderUseCase;
import com.brokerx.order_service.application.port.out.OrderRepositoryPort;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.infrastructure.client.WalletServiceClient;
import com.brokerx.order_service.infrastructure.client.MarketServiceClient.StockResponse;
import com.brokerx.order_service.infrastructure.client.WalletServiceClient.WalletResponse;
import com.brokerx.order_service.infrastructure.client.MarketServiceClient;
import com.brokerx.order_service.infrastructure.kafka.dto.OrderAcceptedEvent;
import com.brokerx.order_service.infrastructure.kafka.producer.OrderEventProducer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class OrderService implements PlaceOrderUseCase, ModifyOrderUseCase, CancelOrderUseCase, GetOrderUseCase {

    private static final Logger logger = LogManager.getLogger(OrderService.class);

    private final OrderRepositoryPort orderRepositoryPort;
    private final WalletServiceClient walletServiceClient;
    private final MarketServiceClient marketServiceClient;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepositoryPort orderRepositoryPort,
            WalletServiceClient walletServiceClient,
            MarketServiceClient marketServiceClient,
            OrderEventProducer orderEventProducer) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.walletServiceClient = walletServiceClient;
        this.marketServiceClient = marketServiceClient;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    public PlaceOrderResponse placeOrder(PlaceOrderCommand command, String ipAddress, String userAgent) {
        logger.info("Placing order: user={}, stock={}, side={}, type={}, qty={}, ip={}",
                command.getUserId(), command.getStockSymbol(), command.getSide(),
                command.getType(), command.getQuantity(), ipAddress);

        // Validate the stock via the market service
        MarketServiceClient.StockResponse stockResponse = marketServiceClient
                .getStockBySymbol(command.getStockSymbol());
        if (stockResponse == null) {
            String reason = "Invalid stock symbol: " + command.getStockSymbol();
            logger.warn("Order rejected: {}", reason);
            return PlaceOrderResponse.rejected(null, reason);
        }

        logger.info("Stock response received: id={}, symbol={}, name={}, price={}",
                stockResponse.id(), stockResponse.symbol(), stockResponse.name(), stockResponse.currentPrice());

        // Retrieve the user's wallet
        WalletServiceClient.WalletResponse walletResponse = walletServiceClient.getWalletByUserId(command.getUserId());
        if (walletResponse == null) {
            String reason = "Wallet not found for user ID: " + command.getUserId();
            logger.warn("Order rejected: {}", reason);
            return PlaceOrderResponse.rejected(null, reason);
        }

        // Calculate the reserved amount
        BigDecimal reservedAmount = calculateReservedAmount(command, stockResponse);

        // Check if wallet has sufficient balance
        if (walletResponse.availableBalance().compareTo(reservedAmount) < 0) {
            String reason = String.format("Insufficient funds. Required: %s, Available: %s", 
                    reservedAmount, walletResponse.availableBalance());
            logger.warn("Order rejected: {}", reason);
            return PlaceOrderResponse.rejected(stockResponse.id(), reason);
        }

        logger.info("Wallet balance check passed: required={}, available={}", 
                reservedAmount, walletResponse.availableBalance());

        // Create the order
        Order order = Order.builder()
                .walletId(walletResponse.id())
                .stockId(stockResponse.id())
                .side(command.getSide())
                .type(command.getType())
                .quantity(command.getQuantity())
                .limitPrice(command.getLimitPrice())
                .executedPrice(command.getType() == OrderType.MARKET ? stockResponse.currentPrice() : null)
                .status(command.getType() == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.ACCEPTED)
                .createdAt(Instant.now())
                .build();

        // Save the order via the repository
        Order savedOrder = orderRepositoryPort.save(order);

        logger.info("Order accepted: id={}, walletId={}, stockId={}",
                savedOrder.getId(), savedOrder.getWalletId(), savedOrder.getStockId());

        // If LIMIT order, publish OrderAccepted event to matching_service for matching
        if (command.getType() == OrderType.LIMIT) {
            logger.info("Publishing OrderAccepted event for LIMIT order: id={}, symbol={}, side={}, qty={}, price={}",
                    savedOrder.getId(), command.getStockSymbol(), command.getSide(), 
                    command.getQuantity(), command.getLimitPrice());

            if(command.getSide() == OrderSide.BUY) {
                // Reserve funds in wallet for BUY LIMIT order
                walletServiceClient.reserveFundsForWallet(walletResponse.id(), reservedAmount, savedOrder.getId());
                logger.info("Reserved {} in wallet {} for BUY LIMIT order {}", reservedAmount, walletResponse.id(), savedOrder.getId());
            }
            
            OrderAcceptedEvent event = new OrderAcceptedEvent(
                    savedOrder.getId(),
                    command.getStockSymbol(),
                    command.getSide().toString(),
                    command.getLimitPrice(),
                    command.getQuantity()
            );
            
            orderEventProducer.publishOrderAccepted(event);
        }

        // If MARKET order and BUY side, execute the transaction immediately and debit the wallet
        if (command.getType() == OrderType.MARKET && command.getSide() == OrderSide.BUY) {
            logger.info("Executing MARKET order: debiting {} from user {}", reservedAmount, command.getUserId());
            //walletServiceClient.debitWallet(command.getUserId(), reservedAmount);
            walletServiceClient.executeBUY(new WalletServiceClient.PositionResponse(
                command.getUserId(),
                command.getStockSymbol(),
                command.getSide().toString(),
                command.getQuantity(),
                stockResponse.currentPrice(),
                savedOrder.getId()
            ));
            logger.info("MARKET order executed successfully for order ID: {}", savedOrder.getId());
        }
        // If MARKET order and SELL side, execute the transaction immediately and credit the wallet
        if (command.getType() == OrderType.MARKET && command.getSide() == OrderSide.SELL) {
            logger.info("Executing MARKET order: crediting {} to user {}", reservedAmount, command.getUserId());
            //walletServiceClient.creditWallet(command.getUserId(), reservedAmount);
            walletServiceClient.executeSELL(new WalletServiceClient.PositionResponse(
                command.getUserId(),
                command.getStockSymbol(),
                command.getSide().toString(),
                command.getQuantity(),
                stockResponse.currentPrice(),
                savedOrder.getId()
            ));
            logger.info("MARKET order executed successfully for order ID: {}", savedOrder.getId());
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
            logger.warn("Invalid order ID format: {}", orderId);
            return Optional.empty();
        }
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        WalletResponse walletResponse = walletServiceClient.getWalletByUserId(userId);
        if (walletResponse == null) {
            logger.warn("Wallet not found for user ID: {}", userId);
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
    public boolean cancelOrder(CancelOrderCommand command) {
        Long orderId = command.getOrderId();
        Long userId = command.getUserId();

        logger.info("Attempting to cancel order {} for user {}", orderId, userId);

        // Fetch the order
        Optional<Order> optOrder = orderRepositoryPort.findById(orderId);
        if (optOrder.isEmpty()) {
            logger.warn("Cancel failed: order {} not found", orderId);
            return false;
        }

        Order order = optOrder.get();

        // Check that it belongs to the user
        WalletResponse wallet = walletServiceClient.getWalletByUserId(userId);
        if (wallet == null || !wallet.id().equals(order.getWalletId())) {
            logger.warn("Cancel failed: order {} does not belong to user {}", orderId, userId);
            return false;
        }

        // Check that it is not already FILLED or CANCELLED
        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            logger.warn("Cancel rejected: order {} already filled or cancelled", orderId);
            return false;
        }

        // Update the status
        order.setStatus(OrderStatus.CANCELLED);
        orderRepositoryPort.save(order);
        logger.info("Order {} cancelled successfully", orderId);

        // If limit buy not executed, refund the reserved amount
        if (order.getSide() == OrderSide.BUY && order.getLimitPrice() != null) {
            BigDecimal refundAmount = order.getLimitPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            walletServiceClient.creditWallet(userId, refundAmount);
            logger.info("Refunded {} to user {} for cancelled order {}", refundAmount, userId, orderId);
        }

        return true;
    }

    @Override
    public boolean modifyOrder(ModifyOrderCommand command) {
        Long orderId = command.getOrderId();
        Long userId = command.getUserId();

        logger.info("Attempting to modify order {} for user {}", orderId, userId);

        Optional<Order> optOrder = orderRepositoryPort.findById(orderId);
        if (optOrder.isEmpty()) {
            logger.warn("Modify failed: order {} not found", orderId);
            return false;
        }

        Order order = optOrder.get();

        WalletResponse wallet = walletServiceClient.getWalletByUserId(userId);
        if (wallet == null || !wallet.id().equals(order.getWalletId())) {
            logger.warn("Modify failed: order {} does not belong to user {}", orderId, userId);
            return false;
        }

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            logger.warn("Modify rejected: order {} already filled or cancelled", orderId);
            return false;
        }

        // Apply new values
        if (command.getNewQuantity() != null) {
            order.setQuantity(command.getNewQuantity());
        }
        if (command.getNewLimitPrice() != 0) {
            order.setLimitPrice(BigDecimal.valueOf(command.getNewLimitPrice()));
        }

        // Recalculate reserved amount in wallet
        orderRepositoryPort.save(order);
        logger.info("Order {} modified successfully", orderId);

        return true;
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
