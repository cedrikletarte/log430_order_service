package com.brokerx.order_service.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.client.HttpClientErrorException;

/**
 * Client to communicate with the Market Service.
 * Makes secure internal HTTP calls via JWT signature.
 */
@Slf4j
@Component
public class MarketServiceClient {

    private final RestTemplate restTemplate;
    private final String marketServiceUrl;
    private final ServiceSignatureGenerator signatureGenerator;

    public MarketServiceClient(
            RestTemplate restTemplate,
            @Value("${market.service.url}") String marketServiceUrl,
            ServiceSignatureGenerator signatureGenerator) {
        this.restTemplate = restTemplate;
        this.marketServiceUrl = marketServiceUrl;
        this.signatureGenerator = signatureGenerator;
    }

    /* Get stock information by symbol (synchronous read operation) */
    public StockResponse getStockBySymbol(String symbol) {
        String url = marketServiceUrl + "/internal/stock/" + symbol;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.debug("Calling market service: GET {}", url);
            
            ResponseEntity<StockResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                StockResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Stock data retrieved for symbol: {}", symbol);
                return response.getBody();
            }
            
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Stock symbol not found: {}", symbol);
            return null;
        } catch (Exception e) {
            log.error("Error calling market service for symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /* Get stock information by ID (synchronous read operation) */
    public StockResponse getStockById(Long stockId) {
        String url = marketServiceUrl + "/internal/stock/id/" + stockId;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Service-Token", signatureGenerator.generateSignature());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.debug("Calling market service: GET {}", url);
            
            ResponseEntity<StockResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                StockResponse.class
            );
        
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Stock data retrieved for id: {}", stockId);
                return response.getBody();
            }
            
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Stock id not found: {}", stockId);
            return null;
        } catch (Exception e) {
            log.error("Error calling market service for stock id {}: {}", stockId, e.getMessage());
            return null;
        }
    }


    /* DTO stock response */
    public record StockResponse(
        Long id,
        String symbol,
        String name,
        java.math.BigDecimal currentPrice
    ) {}
}
