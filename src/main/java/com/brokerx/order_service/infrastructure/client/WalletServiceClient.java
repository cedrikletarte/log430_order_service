package com.brokerx.order_service.infrastructure.client;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Client for communicating with the Wallet Service.
 * Makes secure internal HTTP calls via X-Service-Token.
 */
@Slf4j
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;
    private final String serviceSecret;

    public WalletServiceClient(
            RestTemplate restTemplate,
            @Value("${wallet.service.url}") String walletServiceUrl,
            @Value("${service.secret}") String serviceSecret) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
        this.serviceSecret = serviceSecret;
    }

    public WalletResponse getWalletByUserId(Long userId) {
        String url = walletServiceUrl + "/internal/wallet/" + userId;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", serviceSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.debug("Calling wallet service: GET {}", url);
            
            ResponseEntity<WalletResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                WalletResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Long walletId = response.getBody().id();
                log.debug("Wallet ID retrieved: {} for user ID: {}", walletId, userId);
                return response.getBody();
            }
            
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Wallet not found for user ID: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error calling wallet service for user ID: {}", userId, e);
            throw new RuntimeException("Failed to retrieve wallet ID", e);
        }
    }

    public boolean hasSufficientBalance(Double amount) {
        return true;
    }

    

    /**
     * DTO wallet response
     */
    public record WalletResponse(
        Long id,
        String currency,
        BigDecimal balance
    ) {}
}
