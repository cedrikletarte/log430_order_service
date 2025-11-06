package com.brokerx.order_service.application.port.in.useCase;

import com.brokerx.order_service.application.port.in.command.ModifyOrderCommand;

public interface ModifyOrderUseCase {
    
    boolean modifyOrder(ModifyOrderCommand command);
}
