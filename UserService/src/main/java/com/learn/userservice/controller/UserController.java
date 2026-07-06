package com.learn.userservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/profile")
    public Mono<Map<String, Object>> getUserProfile(
            @RequestHeader(value = "X-Authenticated-User-Role", defaultValue = "UNKNOWN") String role) {
        
        return Mono.just(Map.of(
            "userId", "user-12345",
            "name", "Biswajit Shaw",
            "email", "biswajit@example.com",
            "extractedRoleFromGateway", role,
            "source", "User Backend Microservice"
        ));
    }
}
