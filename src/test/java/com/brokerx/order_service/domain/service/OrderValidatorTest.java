package com.brokerx.order_service.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.brokerx.order_service.domain.exception.OrderException;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.domain.model.OrderType;

class OrderValidatorTest {

    private OrderValidator orderValidator;

    @BeforeEach
    void setUp() {
        orderValidator = new OrderValidator();
    }


    @Test
    void shouldAcceptValidMarketOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .status(OrderStatus.PENDING)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptValidLimitOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("100.50"))
                .status(OrderStatus.PENDING)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectOrderWithNullStockId() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(null)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectOrderWithNullSide() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(null)
                .type(OrderType.MARKET)
                .quantity(50)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectOrderWithNullType() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(null)
                .quantity(50)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectOrderWithNullWalletId() {
        Order order = Order.builder()
                .walletId(null)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    // ===== Tests pour la quantité =====

    @Test
    void shouldAcceptMinimumQuantity() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(1)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptMaximumQuantity() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(1_000_000)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectZeroQuantity() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(0)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectNegativeQuantity() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(-10)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectQuantityAboveMaximum() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(1_000_001)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    // ===== Tests pour le prix des ordres LIMIT =====

    @Test
    void shouldAcceptLimitOrderWithValidPrice() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("100.50"))
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptLimitOrderWithTwoDecimalPlaces() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("99.99"))
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptLimitOrderWithOneDecimalPlace() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("100.5"))
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptLimitOrderWithNoDecimalPlaces() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("100"))
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectLimitOrderWithNullPrice() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(null)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectLimitOrderWithZeroPrice() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(BigDecimal.ZERO)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectLimitOrderWithNegativePrice() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("-50.00"))
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectLimitOrderWithMoreThanTwoDecimalPlaces() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("100.123"))
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectLimitOrderWithThreeDecimalPlaces() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("99.999"))
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    // ===== Tests pour les ordres MARKET =====

    @Test
    void shouldAcceptMarketOrderWithoutLimitPrice() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectMarketOrderWithExecutedPrice() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .executedPrice(new BigDecimal("100.00"))
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    // ===== Tests pour BUY et SELL =====

    @Test
    void shouldAcceptBuyOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptSellOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(50)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptSellLimitOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("150.75"))
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }

    // ===== Tests de combinaisons =====

    @Test
    void shouldRejectLimitOrderWithMultipleIssues() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(0) // Quantité invalide
                .limitPrice(new BigDecimal("-10.00")) // Prix négatif
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldRejectOrderWithMissingRequiredFields() {
        Order order = Order.builder()
                .walletId(null)
                .stockId(null)
                .side(null)
                .type(null)
                .quantity(50)
                .build();

        assertThrows(OrderException.class, () -> orderValidator.validateForCreation(order));
    }

    @Test
    void shouldAcceptCompleteValidOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(500)
                .limitPrice(new BigDecimal("125.50"))
                .status(OrderStatus.PENDING)
                .build();

        assertDoesNotThrow(() -> orderValidator.validateForCreation(order));
    }
}
