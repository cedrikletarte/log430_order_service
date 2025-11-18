package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.PlaceOrderCommand;
import com.brokerx.order_service.application.port.in.command.PlaceOrderResponse;

public interface PlaceOrderUseCase {

    /* Placing a new order */
    PlaceOrderResponse placeOrder(PlaceOrderCommand command, String ipAddress, String userAgent);
}
    
