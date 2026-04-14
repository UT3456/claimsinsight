package com.claiminsight.metrics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures Jackson to serialize LocalDate and LocalDateTime as ISO strings instead of arrays. */
@Configuration
public class JacksonConfig {
    
    /** Registers JavaTimeModule and disables WRITE_DATES_AS_TIMESTAMPS. */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register the module that handles LocalDate, LocalDateTime etc.
        mapper.registerModule(new JavaTimeModule());
        // Write dates as "2026-01-15" not [2026, 1, 15]
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
