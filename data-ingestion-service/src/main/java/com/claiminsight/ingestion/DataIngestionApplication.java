package com.claiminsight.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Entry point for the Data Ingestion Service (Microservice 2, port 8082). */
@SpringBootApplication
@EnableCaching
@EnableFeignClients
public class DataIngestionApplication {
    
    /** Starts the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(DataIngestionApplication.class, args);
    }
}
