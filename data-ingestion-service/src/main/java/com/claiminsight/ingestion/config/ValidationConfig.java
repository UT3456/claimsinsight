package com.claiminsight.ingestion.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Wires error.properties as the message source for Bean Validation constraint messages. */
@Configuration
public class ValidationConfig {
    
    /** Loads validation messages from classpath:error.properties. */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();

        // Points to src/main/resources/error.properties
        messageSource.setBasename("classpath:error");
        messageSource.setDefaultEncoding("UTF-8");

        return messageSource;
    }
    
    /** Registers the message source with the Bean Validation framework. */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }
}
