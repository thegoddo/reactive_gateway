package com.learn.orderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping("/history")
    public Mono<Map<String, Object>> getOrderHistory() {
        return Mono.just(Map.of(
            "source", "Order Backend Microservice",
            "orders", List.of(
                Map.of("orderId", "ORD-99812", "amount", 1500.00, "status", "DELIVERED"),
                Map.of("orderId", "ORD-44120", "amount", 450.50, "status", "PROCESSING")
            )
        ));
    }
}
