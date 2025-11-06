package com.brokerx.order_service.adapter.web.dto;

public record CancelOrderResponse(boolean success, String message, Long orderId) {}
