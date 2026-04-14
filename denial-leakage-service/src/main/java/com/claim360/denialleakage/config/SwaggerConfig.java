package com.claim360.denialleakage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// http://localhost:8085/swagger-ui/index.html

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
    public OpenAPI denialLeakageServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Denial Leakage Service API")
                        .description("Module 4.5 - Denial and Leakage Analysis")
                        .version("1.0.0")
                );
    }
}