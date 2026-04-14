package com.claiminsight.metrics.config;

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
                        .title("Claims Metrics Engine API")
                        .description("ClaimInsight360 — Microservice 3: Records and queries KPI metrics for insurance claims.")
                        .version("1.0.0"));
    }
}
