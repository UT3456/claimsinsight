package com.claims.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Reverts resource-handler path matching to AntPathMatcher so SpringDoc 2.8.x can register
 * /swagger-ui/**\/*swagger-initializer.js without Spring Boot 3.5's PathPatternParser
 * rejecting the ** wildcard followed by additional path data.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    @SuppressWarnings("deprecation")
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setPatternParser(null);
    }
}
