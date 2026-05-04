package com.demo;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class AdjusterAndOperationsApplication implements CommandLineRunner {
    public static void main(String[] args) {

        SpringApplication.run(AdjusterAndOperationsApplication.class, args
        );
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
