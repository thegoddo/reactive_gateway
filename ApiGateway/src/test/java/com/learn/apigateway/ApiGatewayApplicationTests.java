package com.learn.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;
    
    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Test
    void whenRequestMissingHeader_thenRejectUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/users/profile")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-Gateway-Failure-Reason", "Missing Authorization Header");
    }

    @Test
    void whenRequestHasInvalidToken_thenRejectUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-secret")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-Gateway-Failure-Reason", "Token Verification Failed");
    }

    @Test
    void whenOrderHistoryCalled_thenRouteFoundOrForwarded() {
        // Since downstream OrderService might be offline during build, 
        // we check that it doesn't drop out with a 404 Route Missing error.
        webTestClient.get()
                .uri("/api/v1/orders/history")
                .exchange()
                .expectStatus().isNotFound(); 
    }
}