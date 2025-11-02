package com.brokerx.order_service.infrastructure.adapter;

import com.brokerx.order_service.application.port.out.WalletPort;
import com.brokerx.order_service.infrastructure.client.WalletServiceClient;
import com.brokerx.order_service.infrastructure.client.WalletServiceClient.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter that implements WalletPort using REST client
 * This follows hexagonal architecture by adapting infrastructure to application port
 * Only used for synchronous read operations (queries)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletRestAdapter implements WalletPort {
    
    private final WalletServiceClient walletServiceClient;
    
    @Override
    public WalletData getWalletByUserId(Long userId) {
        WalletResponse response = walletServiceClient.getWalletByUserId(userId);
        if (response == null) {
            return null;
        }
        return new WalletData(
            response.id(),
            response.availableBalance(),
            response.reservedBalance(),
            response.currency()
        );
    }
}
