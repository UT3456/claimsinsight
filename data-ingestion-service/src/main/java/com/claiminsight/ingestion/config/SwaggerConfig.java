package com.claiminsight.ingestion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the OpenAPI/Swagger UI documentation for this service. */
@Configuration
public class SwaggerConfig {

    /** Defines the API title, description, and version shown in Swagger UI. */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Data Ingestion Service API")
                        .description("ClaimInsight360 — Microservice 2: Registers feed sources and ingests raw claim payloads.")
                        .version("1.0.0"));
    }
}
