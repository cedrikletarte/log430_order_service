package com.brokerx.order_service.application.port.out;

import java.math.BigDecimal;

public interface WalletPort {
    
    /* Get wallet information for a user (synchronous read operation) */
    WalletData getWalletByUserId(Long userId);
    
    /* DTO for wallet data */
    record WalletData(
        Long id,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        String currency
    ) {}
}
