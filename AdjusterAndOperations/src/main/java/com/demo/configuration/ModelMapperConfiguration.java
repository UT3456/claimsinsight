package com.demo.configuration;

import com.demo.dto.SLAViolationDTO;
import com.demo.entities.SLAViolation;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for ModelMapper.
 */
@Configuration
public class ModelMapperConfiguration {

    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        modelMapper.typeMap(SLAViolation.class, SLAViolationDTO.class)
                .addMappings(mapper -> mapper.map(
                        src -> src.getClaim().getClaimId(),
                        SLAViolationDTO::setClaimId
                ));

        return modelMapper;
    }
}