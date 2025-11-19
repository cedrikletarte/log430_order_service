package com.brokerx.order_service.infrastructure.persistence.mapper;

import com.brokerx.order_service.domain.model.Order;
import com.brokerx.order_service.infrastructure.persistence.entity.OrderEntity;

import org.springframework.stereotype.Component;

/* Mapper for converting between Order domain model and OrderEntity */
@Component
public class OrderMapper {

    public OrderEntity toEntity(Order order) {
        if (order == null)
            return null;
        return OrderEntity.builder()
                .id(order.getId())
                .walletId(order.getWalletId())
                .stockId(order.getStockId())
                .side(order.getSide())
                .type(order.getType())
                .quantity(order.getQuantity())
                .executedQuantity(order.getExecutedQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .limitPrice(order.getLimitPrice())
                .executedPrice(order.getExecutedPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public Order toDomain(OrderEntity entity) {
        if (entity == null)
            return null;
        return Order.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .stockId(entity.getStockId())
                .side(entity.getSide())
                .type(entity.getType())
                .quantity(entity.getQuantity())
                .executedQuantity(entity.getExecutedQuantity())
                .remainingQuantity(entity.getRemainingQuantity())
                .limitPrice(entity.getLimitPrice())
                .executedPrice(entity.getExecutedPrice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
