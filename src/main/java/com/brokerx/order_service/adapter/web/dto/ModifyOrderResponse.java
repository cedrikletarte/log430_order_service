package com.brokerx.order_service.adapter.web.dto;

/* Response for modify order operation */
public record ModifyOrderResponse(boolean success, String message, Long orderId) {}
