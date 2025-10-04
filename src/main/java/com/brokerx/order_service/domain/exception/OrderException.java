package com.brokerx.order_service.domain.exception;

public class OrderException extends RuntimeException {
    
    private final String errorCode;
    
    public OrderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public OrderException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public static OrderException insufficientFunds(Long stockId, java.math.BigDecimal required, java.math.BigDecimal available) {
        return new OrderException("INSUFFICIENT_FUNDS", 
            String.format("Fonds insuffisants pour le stock %s. Requis: %s, Disponible: %s", 
                stockId, required, available));
    }
    
    public static OrderException invalidQuantity(int quantity) {
        return new OrderException("INVALID_QUANTITY", 
            String.format("Quantité invalide: %s. Doit être > 0", quantity));
    }
    
    public static OrderException stockNotActive(Long stockId) {
        return new OrderException("STOCK_NOT_ACTIVE", 
            String.format("Stock non actif: %s", stockId));
    }
    
    public static OrderException orderNotFound(String orderId) {
        return new OrderException("ORDER_NOT_FOUND", 
            String.format("Ordre non trouvé: %s", orderId));
    }
    
    public static OrderException duplicateClientOrderId(String clientOrderId) {
        return new OrderException("DUPLICATE_CLIENT_ORDER_ID", 
            String.format("ID client d'ordre déjà utilisé: %s", clientOrderId));
    }
}