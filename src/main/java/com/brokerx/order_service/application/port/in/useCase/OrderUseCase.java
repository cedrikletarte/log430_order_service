package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.CancelOrderCommand;
import com.brokerx.order_service.application.port.in.command.ModifyOrderCommand;
import com.brokerx.order_service.application.port.in.command.OrderResponse;
import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;

import java.util.List;
import java.util.Optional;

public interface OrderUseCase {

    Optional<OrderResponse> getOrderById(String orderId);

    List<OrderResponse> getOrdersByUserId(Long userId);

    PlaceOrderResponse placeOrder(PlaceOrderCommand command, String ipAddress, String userAgent);

    boolean cancelOrder(CancelOrderCommand command);

    boolean modifyOrder(ModifyOrderCommand command);
}