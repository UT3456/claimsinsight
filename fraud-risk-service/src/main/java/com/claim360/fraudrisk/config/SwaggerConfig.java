package com.claim360.fraudrisk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    /** Reverts resource-handler path matching to AntPathMatcher so SpringDoc's
     *  /swagger-ui/**\/*swagger-initializer.js pattern is accepted by Spring Boot 3.5. */
    @Override
    @SuppressWarnings("deprecation")
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setPatternParser(null);
    }


    @Bean
    public OpenAPI fraudRiskServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud Risk Service API")
                        .description(
                                "Module 4.4 - Fraud Indicators & Risk Scoring Engine. " +
                                        "This service provides REST APIs for managing Risk Indicators " +
                                        "and Risk Scores as part of the ClaimInsight360 platform."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ClaimInsight360 Team")
                                .email("support@claiminsight360.com")
                                .url("https://claiminsight360.com")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8084")
                                .description("Local Development Server")
                ));
    }
}