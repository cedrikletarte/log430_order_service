package com.brokerx.order_service.application.port.out;

import java.math.BigDecimal;

/**
 * Port for wallet operations
 * This follows hexagonal architecture by defining the output port in the application layer
 * Note: Write operations (reserve, credit) should be done via events, not synchronous calls
 */
public interface WalletPort {
    
    /**
     * Get wallet information for a user (synchronous read operation)
     * @param userId The user ID
     * @return WalletData or null if not found
     */
    WalletData getWalletByUserId(Long userId);
    
    /**
     * DTO for wallet data
     */
    record WalletData(
        Long id,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        String currency
    ) {}
}
