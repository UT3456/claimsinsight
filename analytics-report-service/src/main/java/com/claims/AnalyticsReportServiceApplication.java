package com.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.claims")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.claims.client")
public class AnalyticsReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsReportServiceApplication.class, args);
    }
}
