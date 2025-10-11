package com.brokerx.order_service.infrastructure.seeder;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.domain.model.OrderSide;
import com.brokerx.order_service.domain.model.OrderStatus;
import com.brokerx.order_service.domain.model.OrderType;
import com.brokerx.order_service.infrastructure.persistence.repository.order.OrderRepositoryAdapter;

@Configuration
public class OrderSeeder {

        private static final Logger log = LoggerFactory.getLogger(OrderSeeder.class);

    @Bean
    CommandLineRunner seedOrder(OrderRepositoryAdapter orderRepository) {
        return args -> {
            if (orderRepository.findById(1L).isEmpty()) {
                Order order = Order.builder()
                        .walletId(1L)
                        .stockId(1L)
                        .side(OrderSide.BUY)
                        .type(OrderType.MARKET)
                        .quantity(10)
                        .limitPrice(null)
                        .executedPrice(BigDecimal.valueOf(125))
                        .status(OrderStatus.ACCEPTED)
                        .build();
                orderRepository.save(order);
                log.info("Order created with ID {}: {}", order.getId(), order.getStatus());
            }
        };
    }
}
