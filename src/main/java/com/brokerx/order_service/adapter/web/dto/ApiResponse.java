package com.brokerx.order_service.adapter.web.dto;

/* Generic API response wrapper */
public record ApiResponse<T>(
    String status,
    String errorCode,
    String message,
    T data
) {}
