package com.brokerx.order_service.application.port.in.useCase;

import java.util.List;
import java.util.Optional;

import com.brokerx.order_service.application.port.in.command.OrderResponse;

public interface GetOrderUseCase {

    /* Retrieving order details */
    Optional<OrderResponse> getOrderById(String orderId);

    /* Retrieving orders for a specific user */
    List<OrderResponse> getOrdersByUserId(Long userId);
}
