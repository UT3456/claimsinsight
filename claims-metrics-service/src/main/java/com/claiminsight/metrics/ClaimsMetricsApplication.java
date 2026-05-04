package com.claiminsight.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point for the Claims Metrics Engine (Microservice 3, port 8083). */
@SpringBootApplication
@EnableCaching
@EnableFeignClients
@EnableScheduling
public class ClaimsMetricsApplication {

    /** Starts the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(ClaimsMetricsApplication.class, args);
    }
}
