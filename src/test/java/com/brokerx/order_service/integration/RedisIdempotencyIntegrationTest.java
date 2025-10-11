package com.brokerx.order_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.brokerx.order_service.infrastructure.config.RedisConfig;
import com.brokerx.order_service.infrastructure.service.IdempotencyService;

@Testcontainers
@DataRedisTest
@Import({IdempotencyService.class, RedisConfig.class})
class RedisIdempotencyIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        // Nettoyer Redis avant chaque test
        var connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory != null) {
            connectionFactory.getConnection().serverCommands().flushAll();
        }
    }

    @Test
    void shouldDetectNoDuplicateForNewKey() {
        String idempotencyKey = "unique-key-123";
        Long userId = 1L;

        boolean isDuplicate = idempotencyService.isDuplicate(idempotencyKey, userId);
        
        assertFalse(isDuplicate, "New idempotency key should not be detected as duplicate");
    }

    @Test
    void shouldDetectDuplicateAfterStoringResponse() {
        String idempotencyKey = "test-key-456";
        Long userId = 2L;
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", 100L);
        response.put("status", "PENDING");

        // Première requête - stocker la réponse
        idempotencyService.storeResponse(idempotencyKey, userId, response);

        // Deuxième requête - devrait détecter le duplicata
        boolean isDuplicate = idempotencyService.isDuplicate(idempotencyKey, userId);
        
        assertTrue(isDuplicate, "Idempotency key should be detected as duplicate after storing");
    }

    @Test
    void shouldStoreAndRetrieveResponse() {
        String idempotencyKey = "response-key-789";
        Long userId = 3L;
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("orderId", 200L);
        expectedResponse.put("status", "ACCEPTED");
        expectedResponse.put("quantity", 50);

        idempotencyService.storeResponse(idempotencyKey, userId, expectedResponse);

        Object cachedResponse = idempotencyService.getCachedResponse(idempotencyKey, userId);
        
        assertNotNull(cachedResponse, "Cached response should not be null");
        assertTrue(cachedResponse instanceof Map, "Cached response should be a Map");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedResponse = (Map<String, Object>) cachedResponse;
        assertEquals(200, retrievedResponse.get("orderId"));
        assertEquals("ACCEPTED", retrievedResponse.get("status"));
        assertEquals(50, retrievedResponse.get("quantity"));
    }

    @Test
    void shouldReturnNullForNonExistentKey() {
        String idempotencyKey = "non-existent-key";
        Long userId = 4L;

        Object response = idempotencyService.getCachedResponse(idempotencyKey, userId);
        
        assertNull(response, "Response should be null for non-existent key");
    }

    @Test
    void shouldIsolateDifferentUserIds() {
        String idempotencyKey = "same-key";
        Long userId1 = 5L;
        Long userId2 = 6L;
        
        Map<String, Object> response1 = new HashMap<>();
        response1.put("orderId", 300L);
        
        Map<String, Object> response2 = new HashMap<>();
        response2.put("orderId", 400L);

        // Stocker pour deux utilisateurs différents avec la même clé
        idempotencyService.storeResponse(idempotencyKey, userId1, response1);
        idempotencyService.storeResponse(idempotencyKey, userId2, response2);

        // Vérifier l'isolation
        assertTrue(idempotencyService.isDuplicate(idempotencyKey, userId1));
        assertTrue(idempotencyService.isDuplicate(idempotencyKey, userId2));

        @SuppressWarnings("unchecked")
        Map<String, Object> cached1 = (Map<String, Object>) idempotencyService.getCachedResponse(idempotencyKey, userId1);
        @SuppressWarnings("unchecked")
        Map<String, Object> cached2 = (Map<String, Object>) idempotencyService.getCachedResponse(idempotencyKey, userId2);

        assertEquals(300, cached1.get("orderId"));
        assertEquals(400, cached2.get("orderId"));
    }

    @Test
    void shouldHandleMultipleIdempotencyKeys() {
        Long userId = 7L;
        
        Map<String, Object> response1 = new HashMap<>();
        response1.put("orderId", 500L);
        
        Map<String, Object> response2 = new HashMap<>();
        response2.put("orderId", 600L);
        
        Map<String, Object> response3 = new HashMap<>();
        response3.put("orderId", 700L);

        idempotencyService.storeResponse("key-1", userId, response1);
        idempotencyService.storeResponse("key-2", userId, response2);
        idempotencyService.storeResponse("key-3", userId, response3);

        assertTrue(idempotencyService.isDuplicate("key-1", userId));
        assertTrue(idempotencyService.isDuplicate("key-2", userId));
        assertTrue(idempotencyService.isDuplicate("key-3", userId));
        assertFalse(idempotencyService.isDuplicate("key-4", userId));
    }

    @Test
    void shouldOverwriteExistingIdempotencyKey() {
        String idempotencyKey = "overwrite-key";
        Long userId = 8L;
        
        Map<String, Object> oldResponse = new HashMap<>();
        oldResponse.put("orderId", 800L);
        oldResponse.put("status", "PENDING");
        
        Map<String, Object> newResponse = new HashMap<>();
        newResponse.put("orderId", 800L);
        newResponse.put("status", "FILLED");

        // Stocker la première réponse
        idempotencyService.storeResponse(idempotencyKey, userId, oldResponse);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cached1 = (Map<String, Object>) idempotencyService.getCachedResponse(idempotencyKey, userId);
        assertEquals("PENDING", cached1.get("status"));

        // Écraser avec une nouvelle réponse
        idempotencyService.storeResponse(idempotencyKey, userId, newResponse);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cached2 = (Map<String, Object>) idempotencyService.getCachedResponse(idempotencyKey, userId);
        assertEquals("FILLED", cached2.get("status"));
    }

    @Test
    void shouldHandleComplexResponseObjects() {
        String idempotencyKey = "complex-key";
        Long userId = 9L;
        
        Map<String, Object> complexResponse = new HashMap<>();
        complexResponse.put("orderId", 900L);
        complexResponse.put("walletId", 100L);
        complexResponse.put("stockId", 200L);
        complexResponse.put("quantity", 75);
        complexResponse.put("price", 125.50);
        complexResponse.put("status", "ACCEPTED");
        complexResponse.put("side", "BUY");
        complexResponse.put("type", "LIMIT");

        idempotencyService.storeResponse(idempotencyKey, userId, complexResponse);

        @SuppressWarnings("unchecked")
        Map<String, Object> retrieved = (Map<String, Object>) idempotencyService.getCachedResponse(idempotencyKey, userId);
        
        assertNotNull(retrieved);
        assertEquals(900, retrieved.get("orderId"));
        assertEquals(100, retrieved.get("walletId"));
        assertEquals(200, retrieved.get("stockId"));
        assertEquals(75, retrieved.get("quantity"));
        assertEquals(125.50, retrieved.get("price"));
        assertEquals("ACCEPTED", retrieved.get("status"));
        assertEquals("BUY", retrieved.get("side"));
        assertEquals("LIMIT", retrieved.get("type"));
    }

    @Test
    void shouldHandleStringResponse() {
        String idempotencyKey = "string-key";
        Long userId = 10L;
        String response = "Order created successfully";

        idempotencyService.storeResponse(idempotencyKey, userId, response);

        Object cached = idempotencyService.getCachedResponse(idempotencyKey, userId);
        
        assertNotNull(cached);
        assertEquals(response, cached);
        assertTrue(idempotencyService.isDuplicate(idempotencyKey, userId));
    }

    @Test
    void shouldHandleNumericResponse() {
        String idempotencyKey = "numeric-key";
        Long userId = 11L;
        Long orderId = 12345L;

        idempotencyService.storeResponse(idempotencyKey, userId, orderId);

        Object cached = idempotencyService.getCachedResponse(idempotencyKey, userId);
        
        assertNotNull(cached);
        assertEquals(12345, cached);
        assertTrue(idempotencyService.isDuplicate(idempotencyKey, userId));
    }

    @Test
    void shouldNotDetectDuplicateAcrossDifferentUsers() {
        String idempotencyKey = "shared-key";
        Long userId1 = 12L;
        Long userId2 = 13L;
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", 1000L);

        // Stocker seulement pour userId1
        idempotencyService.storeResponse(idempotencyKey, userId1, response);

        // userId1 devrait détecter un duplicata
        assertTrue(idempotencyService.isDuplicate(idempotencyKey, userId1));
        
        // userId2 ne devrait PAS détecter de duplicata
        assertFalse(idempotencyService.isDuplicate(idempotencyKey, userId2));
    }
}
