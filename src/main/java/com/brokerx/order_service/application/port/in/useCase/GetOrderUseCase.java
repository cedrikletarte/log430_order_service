package com.brokerx.order_service.application.port.in.useCase;

import java.util.List;
import java.util.Optional;

import com.brokerx.order_service.application.port.in.command.OrderResponse;

public interface GetOrderUseCase {

    Optional<OrderResponse> getOrderById(String orderId);

    List<OrderResponse> getOrdersByUserId(Long userId);
}
