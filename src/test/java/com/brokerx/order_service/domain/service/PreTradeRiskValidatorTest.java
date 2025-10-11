package com.brokerx.order_service.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.brokerx.order_service.domain.exception.OrderException;
import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderType;

class PreTradeRiskValidatorTest {

    private PreTradeRiskValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PreTradeRiskValidator();
    }


    @Test
    void shouldAcceptBuyOrderWithSufficientBalance() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal availableBalance = new BigDecimal("1000.00");
        BigDecimal currentPrice = new BigDecimal("100.00");

        assertDoesNotThrow(() -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldAcceptBuyOrderWithExactBalance() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal availableBalance = new BigDecimal("1000.00"); // Exact amount needed
        BigDecimal currentPrice = new BigDecimal("100.00");

        assertDoesNotThrow(() -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldRejectBuyOrderWithInsufficientBalance() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal availableBalance = new BigDecimal("500.00"); // Not enough
        BigDecimal currentPrice = new BigDecimal("100.00");

        assertThrows(OrderException.class, () -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldAcceptMarketBuyOrderWithSufficientBalance() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .build();

        // For MARKET orders, a 5% safety margin is applied
        BigDecimal availableBalance = new BigDecimal("1100.00");
        BigDecimal currentPrice = new BigDecimal("100.00");

        assertDoesNotThrow(() -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldRejectMarketBuyOrderWithInsufficientBalanceForMargin() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .build();

        // MARKET needs 10 * 100 * 1.05 = 1050
        BigDecimal availableBalance = new BigDecimal("1000.00"); // Not enough with margin
        BigDecimal currentPrice = new BigDecimal("100.00");

        assertThrows(OrderException.class, () -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldSkipPurchasingPowerCheckForSellOrders() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal availableBalance = BigDecimal.ZERO; // No balance needed for selling
        BigDecimal currentPrice = new BigDecimal("100.00");

        assertDoesNotThrow(() -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldHandleLargeQuantityBuyOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(1000)
                .limitPrice(new BigDecimal("50.00"))
                .build();

        BigDecimal availableBalance = new BigDecimal("50000.00");
        BigDecimal currentPrice = new BigDecimal("50.00");

        assertDoesNotThrow(() -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    @Test
    void shouldRejectLargeQuantityBuyOrderWithInsufficientBalance() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(1000)
                .limitPrice(new BigDecimal("50.00"))
                .build();

        BigDecimal availableBalance = new BigDecimal("25000.00"); // Only half needed
        BigDecimal currentPrice = new BigDecimal("50.00");

        assertThrows(OrderException.class, () -> validator.validatePurchasingPower(order, availableBalance, currentPrice));
    }

    // ===== Tests pour validatePriceBands =====

    @Test
    void shouldAcceptLimitOrderWithinPriceBands() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("105.00")) // Within 10% of 100
                .build();

        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal maxDeviationPercent = new BigDecimal("10"); // 10%

        assertDoesNotThrow(() -> validator.validatePriceBands(order, currentPrice, maxDeviationPercent));
    }

    @Test
    void shouldAcceptLimitOrderAtUpperBound() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("110.00")) // Exactly 10% above
                .build();

        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal maxDeviationPercent = new BigDecimal("10");

        assertDoesNotThrow(() -> validator.validatePriceBands(order, currentPrice, maxDeviationPercent));
    }

    @Test
    void shouldAcceptLimitOrderAtLowerBound() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("90.00")) // Exactly 10% below
                .build();

        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal maxDeviationPercent = new BigDecimal("10");

        assertDoesNotThrow(() -> validator.validatePriceBands(order, currentPrice, maxDeviationPercent));
    }

    @Test
    void shouldSkipPriceBandCheckForMarketOrders() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .build();

        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal maxDeviationPercent = new BigDecimal("10");

        assertDoesNotThrow(() -> validator.validatePriceBands(order, currentPrice, maxDeviationPercent));
    }

    @Test
    void shouldSkipPriceBandCheckWhenCurrentPriceIsNull() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("200.00"))
                .build();

        BigDecimal currentPrice = null; // No reference price
        BigDecimal maxDeviationPercent = new BigDecimal("10");

        assertDoesNotThrow(() -> validator.validatePriceBands(order, currentPrice, maxDeviationPercent));
    }

    @Test
    void shouldAcceptLimitOrderWithTightBands() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("102.00")) // Within 5%
                .build();

        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal maxDeviationPercent = new BigDecimal("5"); // Tighter bands

        assertDoesNotThrow(() -> validator.validatePriceBands(order, currentPrice, maxDeviationPercent));
    }

    // ===== Tests pour validateUserLimits =====

    @Test
    void shouldAcceptOrderWithinDailyVolumeLimit() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = new BigDecimal("5000.00");
        BigDecimal maxDailyVolume = new BigDecimal("10000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("5000.00");

        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldAcceptOrderAtDailyVolumeLimit() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = new BigDecimal("5000.00");
        BigDecimal maxDailyVolume = new BigDecimal("10000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("5000.00");

        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldRejectOrderExceedingDailyVolumeLimit() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(100)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = new BigDecimal("5000.00");
        BigDecimal maxDailyVolume = new BigDecimal("10000.00"); // Current (5000) + Order (10000) = 15000 > 10000
        BigDecimal maxSingleOrderNotional = new BigDecimal("20000.00");

        assertThrows(OrderException.class, () -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldAcceptOrderWithinSingleOrderNotionalLimit() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = BigDecimal.ZERO;
        BigDecimal maxDailyVolume = new BigDecimal("100000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("1000.00");

        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldAcceptOrderAtSingleOrderNotionalLimit() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = BigDecimal.ZERO;
        BigDecimal maxDailyVolume = new BigDecimal("100000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("1000.00"); // Exactly the order notional

        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldRejectOrderExceedingSingleOrderNotionalLimit() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(100)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = BigDecimal.ZERO;
        BigDecimal maxDailyVolume = new BigDecimal("100000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("5000.00"); // Order is 10000

        assertThrows(OrderException.class, () -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldAcceptMarketOrderWithinLimits() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .build();

        BigDecimal userDailyVolume = BigDecimal.ZERO;
        BigDecimal maxDailyVolume = new BigDecimal("100000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("50000.00");

        // MARKET orders have null notional, so limits can't be checked
        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldRejectOrderWhenBothLimitsExceeded() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(200)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = new BigDecimal("50000.00");
        BigDecimal maxDailyVolume = new BigDecimal("60000.00"); // Would exceed
        BigDecimal maxSingleOrderNotional = new BigDecimal("10000.00"); // Would exceed

        assertThrows(OrderException.class, () -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldAcceptSmallOrderForNewUser() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(1)
                .limitPrice(new BigDecimal("10.00"))
                .build();

        BigDecimal userDailyVolume = BigDecimal.ZERO; // New user
        BigDecimal maxDailyVolume = new BigDecimal("10000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("1000.00");

        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }

    @Test
    void shouldAcceptSellOrderWithinLimits() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("100.00"))
                .build();

        BigDecimal userDailyVolume = new BigDecimal("2000.00");
        BigDecimal maxDailyVolume = new BigDecimal("10000.00");
        BigDecimal maxSingleOrderNotional = new BigDecimal("5000.00");

        assertDoesNotThrow(() -> validator.validateUserLimits(order, userDailyVolume, maxDailyVolume, maxSingleOrderNotional));
    }
}
