package com.brokerx.order_service.adapter.web.dto;

/* Response for cancel order operation */
public record CancelOrderResponse(boolean success, String message, Long orderId) {}
