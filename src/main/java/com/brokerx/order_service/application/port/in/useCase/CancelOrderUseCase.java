package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.CancelOrderCommand;

public interface CancelOrderUseCase {
    
    /* Canceling an order */
    boolean cancelOrder(CancelOrderCommand command);
}
