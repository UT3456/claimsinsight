package com.claiminsight.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the ClaimInsight360 API Gateway with embedded Identity Module (port 8080).
 * ReactiveUserDetailsServiceAutoConfiguration is excluded because we use JWT-based stateless
 * authentication — no ReactiveUserDetailsService is needed and Spring Boot's default one conflicts.
 */
@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
@EnableAsync
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
