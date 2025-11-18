package com.brokerx.order_service.application.port.in.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/* Command object for canceling an order */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand {
    Long userId;
    Long orderId;
}
