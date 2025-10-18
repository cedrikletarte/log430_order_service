package com.brokerx.order_service.infrastructure.client;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates JWT signatures for service-to-service authentication.
 * This ensures that the service secret never travels over the network.
 */
@Slf4j
@Component
public class ServiceSignatureGenerator {

    private final String serviceSecret;
    private final String serviceName;

    public ServiceSignatureGenerator(
            @Value("${service.secret}") String serviceSecret,
            @Value("${spring.application.name}") String serviceName) {
        this.serviceSecret = serviceSecret;
        this.serviceName = serviceName;
    }

    /**
     * Generates a signed JWT token for service-to-service calls.
     */
    public String generateSignature() {
        long timestamp = System.currentTimeMillis();
        String data = serviceName + ":" + timestamp;
        
        return Jwts.builder()
            .subject("service-auth")
            .claim("service", serviceName)
            .claim("timestamp", timestamp)
            .claim("data", data)
            .signWith(Keys.hmacShaKeyFor(serviceSecret.getBytes()))
            .compact();
    }
}
