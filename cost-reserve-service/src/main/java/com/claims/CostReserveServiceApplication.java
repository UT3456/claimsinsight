package com.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = "com.claims")
@EnableJpaRepositories(basePackages = "com.claims.repository")
@EntityScan(basePackages = "com.claims.entity")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.claims.client")
@EnableScheduling
public class CostReserveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostReserveServiceApplication.class, args);
    }
}



