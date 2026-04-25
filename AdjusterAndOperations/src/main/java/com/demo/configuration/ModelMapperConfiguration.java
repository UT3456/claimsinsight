package com.demo.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for ModelMapper.
 *
 * <p>With STRICT matching, SLAViolation.claimId → SLAViolationDTO.claimId maps
 * automatically by name now that SLAViolation holds a plain {@code Long claimId}
 * instead of a JPA {@code @ManyToOne Claim} relationship. No explicit typeMap
 * override is required.</p>
 */
@Configuration
public class ModelMapperConfiguration {

    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        return modelMapper;
    }
}
