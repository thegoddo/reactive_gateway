package com.learn.orderservice;

import com.learn.orderservice.controller.OrderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(OrderController.class)
class OrderControllerTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenGetOrderHistory_thenReturnMockDataSuccess() {
        webTestClient.get()
                .uri("/api/v1/orders/history")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.source").isEqualTo("Order Backend Microservice")
                .jsonPath("$.orders[0].orderId").isEqualTo("ORD-99812")
                .jsonPath("$.orders[0].status").isEqualTo("DELIVERED");
    }
}