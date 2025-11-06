package com.brokerx.order_service.adapter.web.dto;

public record ModifyOrderResponse(boolean success, String message, Long orderId) {}
