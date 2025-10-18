package com.brokerx.order_service.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.infrastructure.persistence.mapper.OrderMapper;
import com.brokerx.order_service.infrastructure.persistence.repository.order.OrderRepositoryAdapter;

@Testcontainers
@DataJpaTest
@Import({OrderRepositoryAdapter.class, OrderMapper.class})
class OrderRepositoryIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private OrderRepositoryAdapter orderRepository;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldSaveAndRetrieveOrder() {
        Order order = Order.builder()
                .walletId(1L)
                .stockId(100L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        assertNotNull(saved.getId());
        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1L, found.getWalletId().longValue());
        assertEquals(100L, found.getStockId().longValue());
        assertEquals(OrderSide.BUY, found.getSide());
        assertEquals(OrderType.MARKET, found.getType());
        assertEquals(10, found.getQuantity());
        assertEquals(OrderStatus.PENDING, found.getStatus());
    }

    @Test
    void shouldSaveLimitOrder() {
        Order limitOrder = Order.builder()
                .walletId(2L)
                .stockId(200L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(25)
                .limitPrice(BigDecimal.valueOf(150.50))
                .status(OrderStatus.ACCEPTED)
                .build();

        Order saved = orderRepository.save(limitOrder);

        assertNotNull(saved.getId());
        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderType.LIMIT, found.getType());
        assertEquals(0, BigDecimal.valueOf(150.50).compareTo(found.getLimitPrice()));
        assertEquals(OrderStatus.ACCEPTED, found.getStatus());
    }

    @Test
    void shouldSaveMarketOrder() {
        Order marketOrder = Order.builder()
                .walletId(3L)
                .stockId(300L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(50)
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(marketOrder);

        assertNotNull(saved.getId());
        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderType.MARKET, found.getType());
        assertEquals(OrderStatus.PENDING, found.getStatus());
    }

    @Test
    void shouldFindOrdersByWalletId() {
        // Create multiple orders for the same wallet
        Order order1 = Order.builder()
                .walletId(4L)
                .stockId(400L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(10)
                .status(OrderStatus.FILLED)
                .executedPrice(BigDecimal.valueOf(100.00))
                .build();

        Order order2 = Order.builder()
                .walletId(4L)
                .stockId(500L)
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .quantity(20)
                .limitPrice(BigDecimal.valueOf(200.00))
                .status(OrderStatus.PENDING)
                .build();

        Order order3 = Order.builder()
                .walletId(5L) // Different wallet
                .stockId(600L)
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(15)
                .status(OrderStatus.ACCEPTED)
                .build();

        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        List<Order> wallet4Orders = orderRepository.findByWalletId(4L);
        
        assertEquals(2, wallet4Orders.size());
        assertTrue(wallet4Orders.stream()
                .allMatch(o -> o.getWalletId().equals(4L)));
    }

    @Test
    void shouldUpdateOrderStatus() {
        Order order = Order.builder()
                .walletId(6L)
                .stockId(700L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(30)
                .limitPrice(BigDecimal.valueOf(75.25))
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        // Update status from PENDING to ACCEPTED
        saved.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(saved);

        Order updated = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.ACCEPTED, updated.getStatus());
    }

    @Test
    void shouldFillOrderWithExecutedPrice() {
        Order order = Order.builder()
                .walletId(7L)
                .stockId(800L)
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(40)
                .status(OrderStatus.ACCEPTED)
                .build();

        Order saved = orderRepository.save(order);

        // Fill the order with an execution price
        saved.setStatus(OrderStatus.FILLED);
        saved.setExecutedPrice(BigDecimal.valueOf(125.75));
        orderRepository.save(saved);

        Order filled = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.FILLED, filled.getStatus());
        assertEquals(0, BigDecimal.valueOf(125.75).compareTo(filled.getExecutedPrice()));
    }

    @Test
    void shouldRejectOrder() {
        Order order = Order.builder()
                .walletId(8L)
                .stockId(900L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(5)
                .limitPrice(BigDecimal.valueOf(50.00))
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        // Reject the order
        saved.setStatus(OrderStatus.REJECTED);
        orderRepository.save(saved);

        Order rejected = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.REJECTED, rejected.getStatus());
    }

    @Test
    void shouldHandleBuyOrders() {
        Order buyOrder = Order.builder()
                .walletId(10L)
                .stockId(1100L)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(100)
                .limitPrice(BigDecimal.valueOf(99.99))
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(buyOrder);

        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderSide.BUY, found.getSide());
    }

    @Test
    void shouldHandleSellOrders() {
        Order sellOrder = Order.builder()
                .walletId(11L)
                .stockId(1200L)
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(75)
                .status(OrderStatus.ACCEPTED)
                .build();

        Order saved = orderRepository.save(sellOrder);

        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderSide.SELL, found.getSide());
    }
}
