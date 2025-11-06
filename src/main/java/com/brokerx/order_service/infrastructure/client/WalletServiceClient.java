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
 * Makes secure internal HTTP calls via JWT signature.
 */
@Slf4j
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;
    private final ServiceSignatureGenerator signatureGenerator;

    public WalletServiceClient(
            RestTemplate restTemplate,
            @Value("${wallet.service.url}") String walletServiceUrl,
            ServiceSignatureGenerator signatureGenerator) {
        this.restTemplate = restTemplate;
        this.walletServiceUrl = walletServiceUrl;
        this.signatureGenerator = signatureGenerator;
    }

    public WalletResponse getWalletByUserId(Long userId) {
        String url = walletServiceUrl + "/internal/wallet/" + userId;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
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

    /**
     * Get user ID from wallet ID
     * Since walletId == userId in this system (1-to-1 relationship)
     */
    public Long getUserIdByWalletId(Long walletId) {
        // In this system, walletId and userId are the same (1-to-1 mapping)
        // The wallet endpoint /internal/wallet/{userId} uses userId as path param
        // and the returned wallet.id equals the userId
        return walletId;
    }

    /**
     * Debit the wallet of a user by a specified amount.
     * Used when executing a MARKET order.
     */
    public void debitWallet(Long userId, BigDecimal amount) {
        String url = walletServiceUrl + "/internal/wallet/debit/" + userId + "/" + amount;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.debug("Calling wallet service to debit: POST {}", url);
            
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
            );
            
            log.info("Successfully debited {} from user {}", amount, userId);
        } catch (Exception e) {
            log.error("Error debiting wallet for user ID: {}, amount: {}", userId, amount, e);
            throw new RuntimeException("Failed to debit wallet", e);
        }
    }

    /**
     * Credit the wallet of a user by a specified amount.
     * Used when executing a MARKET order.
     */
    public void creditWallet(Long userId, BigDecimal amount) {
        String url = walletServiceUrl + "/internal/wallet/credit/" + userId + "/" + amount;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.debug("Calling wallet service to debit: POST {}", url);
            
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
            );
            
            log.info("Successfully debited {} from user {}", amount, userId);
        } catch (Exception e) {
            log.error("Error debiting wallet for user ID: {}, amount: {}", userId, amount, e);
            throw new RuntimeException("Failed to debit wallet", e);
        }
    }

    public void executeBUY(PositionResponse entity) {
        String url = walletServiceUrl + "/internal/wallet/execute/buy";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PositionResponse> requestEntity = new HttpEntity<>(entity, headers);

            log.debug("Calling wallet service to execute BUY: POST {}", url);

            restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Void.class
            );

            log.info("Successfully executed BUY for {}", entity);
        } catch (Exception e) {
            log.error("Error executing BUY for {}: {}", entity, e.getMessage());
            throw new RuntimeException("Failed to execute BUY", e);
        }
    }

    public void executeSELL(PositionResponse entity) {
        String url = walletServiceUrl + "/internal/wallet/execute/sell";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PositionResponse> requestEntity = new HttpEntity<>(entity, headers);

            log.debug("Calling wallet service to execute SELL: POST {}", url);

            restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Void.class
            );

            log.info("Successfully executed SELL for {}", entity);
        } catch (Exception e) {
            log.error("Error executing SELL for {}: {}", entity, e.getMessage());
            throw new RuntimeException("Failed to execute SELL", e);
        }
    }

    public void reserveFundsForWallet(Long userId, BigDecimal amount, Long orderId) {
        String url = walletServiceUrl + "/internal/wallet/reserve/" + userId + "/" + amount + "/" + orderId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("Calling wallet service to reserve funds: POST {}", url);

            restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
            );

            log.info("Successfully reserved {} for user {}", amount, userId);
        } catch (Exception e) {
            log.error("Error reserving funds for user ID: {}, amount: {}", userId, amount, e);
            throw new RuntimeException("Failed to reserve funds", e);
        }
    }


    /**
     * DTO wallet response
     */
    public record WalletResponse(
        Long id,
        String currency,
        BigDecimal availableBalance,
        BigDecimal reservedBalance
    ) {}

    public record PositionResponse(
        Long userId,
        String symbol,
        String side,
        int quantity,
        BigDecimal price,
        Long orderId
    ) {}
}
