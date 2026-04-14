package com.claiminsight.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** Entry point for the ClaimInsight360 Service Registry (Eureka Server, port 8761). */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /** Starts the Eureka Server. */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
