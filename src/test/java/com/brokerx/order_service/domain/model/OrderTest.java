package com.brokerx.order_service.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldCreateMarketBuyOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .status(OrderStatus.PENDING)
                .build();

        assertNotNull(order);
        assertEquals(1L, order.getWalletId());
        assertEquals(100L, order.getStockId());
        assertEquals(OrderSide.BUY, order.getSide());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(50, order.getQuantity());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNull(order.getLimitPrice());
        assertNull(order.getExecutedPrice());
    }

    @Test
    void shouldCreateLimitSellOrder() {
        Order order = Order.builder()
                .walletId(2L)
                .stockId(200L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(100)
                .limitPrice(new BigDecimal("150.50"))
                .status(OrderStatus.PENDING)
                .build();

        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(OrderType.LIMIT, order.getType());
        assertEquals(0, new BigDecimal("150.50").compareTo(order.getLimitPrice()));
        assertNull(order.getExecutedPrice());
    }

    @Test
    void shouldUpdateOrderStatus() {
        Order order = Order.builder()
                .walletId(3L)
                .stockId(300L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(25)
                .status(OrderStatus.PENDING)
                .build();

        assertEquals(OrderStatus.PENDING, order.getStatus());

        // Mise à jour vers ACCEPTED
        order.setStatus(OrderStatus.ACCEPTED);
        assertEquals(OrderStatus.ACCEPTED, order.getStatus());

        // Mise à jour vers FILLED
        order.setStatus(OrderStatus.FILLED);
        order.setExecutedPrice(new BigDecimal("99.75"));
        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertEquals(0, new BigDecimal("99.75").compareTo(order.getExecutedPrice()));
    }

    @Test
    void shouldHandleRejectedOrder() {
        Order order = Order.builder()
                .walletId(4L)
                .stockId(400L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .limitPrice(new BigDecimal("50.00"))
                .status(OrderStatus.PENDING)
                .build();

        order.setStatus(OrderStatus.REJECTED);
        
        assertEquals(OrderStatus.REJECTED, order.getStatus());
        assertNull(order.getExecutedPrice());
    }

    @Test
    void shouldHandleAllOrderStatuses() {
        Order order = Order.builder()
                .walletId(5L)
                .stockId(500L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(30)
                .build();

        order.setStatus(OrderStatus.PENDING);
        assertEquals(OrderStatus.PENDING, order.getStatus());

        order.setStatus(OrderStatus.ACCEPTED);
        assertEquals(OrderStatus.ACCEPTED, order.getStatus());

        order.setStatus(OrderStatus.FILLED);
        assertEquals(OrderStatus.FILLED, order.getStatus());

        order.setStatus(OrderStatus.REJECTED);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void shouldHandleBothOrderSides() {
        Order buyOrder = Order.builder()
                .walletId(6L)
                .stockId(600L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(20)
                .status(OrderStatus.PENDING)
                .build();

        Order sellOrder = Order.builder()
                .walletId(6L)
                .stockId(600L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(15)
                .limitPrice(new BigDecimal("125.00"))
                .status(OrderStatus.PENDING)
                .build();

        assertEquals(OrderSide.BUY, buyOrder.getSide());
        assertEquals(OrderSide.SELL, sellOrder.getSide());
    }

    @Test
    void shouldHandleBothOrderTypes() {
        Order marketOrder = Order.builder()
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(40)
                .build();

        Order limitOrder = Order.builder()
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(35)
                .limitPrice(new BigDecimal("200.00"))
                .build();

        assertEquals(OrderType.MARKET, marketOrder.getType());
        assertEquals(OrderType.LIMIT, limitOrder.getType());
    }

    @Test
    void shouldHandleDifferentQuantities() {
        Order smallOrder = Order.builder()
                .quantity(1)
                .build();

        Order largeOrder = Order.builder()
                .quantity(10000)
                .build();

        assertEquals(1, smallOrder.getQuantity());
        assertEquals(10000, largeOrder.getQuantity());
    }

    @Test
    void shouldHandleDecimalPrices() {
        Order order = Order.builder()
                .walletId(7L)
                .stockId(700L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(50)
                .limitPrice(new BigDecimal("123.45"))
                .status(OrderStatus.PENDING)
                .build();

        assertEquals(0, new BigDecimal("123.45").compareTo(order.getLimitPrice()));
    }

    @Test
    void shouldSetExecutedPriceOnFill() {
        Order order = Order.builder()
                .walletId(8L)
                .stockId(800L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(75)
                .status(OrderStatus.ACCEPTED)
                .build();

        assertNull(order.getExecutedPrice());

        // Remplir l'ordre
        order.setStatus(OrderStatus.FILLED);
        order.setExecutedPrice(new BigDecimal("88.50"));

        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertEquals(0, new BigDecimal("88.50").compareTo(order.getExecutedPrice()));
    }

    @Test
    void shouldCreateOrderWithAllFields() {
        Order order = Order.builder()
                .id(999L)
                .walletId(9L)
                .stockId(900L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(120)
                .limitPrice(new BigDecimal("175.25"))
                .executedPrice(new BigDecimal("175.30"))
                .status(OrderStatus.FILLED)
                .build();

        assertEquals(999L, order.getId());
        assertEquals(9L, order.getWalletId());
        assertEquals(900L, order.getStockId());
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(OrderType.LIMIT, order.getType());
        assertEquals(120, order.getQuantity());
        assertEquals(0, new BigDecimal("175.25").compareTo(order.getLimitPrice()));
        assertEquals(0, new BigDecimal("175.30").compareTo(order.getExecutedPrice()));
        assertEquals(OrderStatus.FILLED, order.getStatus());
    }

    @Test
    void shouldCreateOrderWithNoArgConstructor() {
        Order order = new Order();
        
        assertNotNull(order);
        assertNull(order.getId());
        assertNull(order.getWalletId());
        assertNull(order.getStockId());
        assertNull(order.getSide());
        assertNull(order.getType());
        assertEquals(0, order.getQuantity());
        assertNull(order.getLimitPrice());
        assertNull(order.getExecutedPrice());
        assertNull(order.getStatus());
    }

    @Test
    void shouldSetAndGetAllFields() {
        Order order = new Order();
        
        order.setId(10L);
        order.setWalletId(100L);
        order.setStockId(1000L);
        order.setSide(OrderSide.BUY);
        order.setType(OrderType.LIMIT);
        order.setQuantity(250);
        order.setLimitPrice(new BigDecimal("99.99"));
        order.setExecutedPrice(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.FILLED);

        assertEquals(10L, order.getId());
        assertEquals(100L, order.getWalletId());
        assertEquals(1000L, order.getStockId());
        assertEquals(OrderSide.BUY, order.getSide());
        assertEquals(OrderType.LIMIT, order.getType());
        assertEquals(250, order.getQuantity());
        assertEquals(0, new BigDecimal("99.99").compareTo(order.getLimitPrice()));
        assertEquals(0, new BigDecimal("100.00").compareTo(order.getExecutedPrice()));
        assertEquals(OrderStatus.FILLED, order.getStatus());
    }
}
