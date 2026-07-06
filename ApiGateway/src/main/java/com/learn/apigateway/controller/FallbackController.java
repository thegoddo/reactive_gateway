package com.learn.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback/user-service")
    public Mono<Map<String, Object>> userServiceFallback() {
        return Mono.just(Map.of(
                "status", "Service Degraded",
                "message", "The downstream user microservice is taking too long to respond. Circuit Breaker is active.",
                "fallbackActive", true
        ));
    }
}