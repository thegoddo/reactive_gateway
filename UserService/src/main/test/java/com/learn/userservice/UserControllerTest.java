package com.learn.userservice;

import com.learn.userservice.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(UserController.class)
class UserControllerTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenGetUserProfileWithGatewayHeader_thenReturnSuccessWithRole() {
        webTestClient.get()
                .uri("/api/v1/users/profile")
                .header("X-Authenticated-User-Role", "ADMIN") // Simulating Gateway injection
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo("user-12345")
                .jsonPath("$.name").isEqualTo("Biswajit Shaw")
                .jsonPath("$.extractedRoleFromGateway").isEqualTo("ADMIN")
                .jsonPath("$.source").isEqualTo("User Backend Microservice");
    }

    @Test
    void whenGetUserProfileWithoutGatewayHeader_thenReturnFallbackUnknownRole() {
        webTestClient.get()
                .uri("/api/v1/users/profile")
                // No header provided
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.extractedRoleFromGateway").isEqualTo("UNKNOWN");
    }
}