package com.claiminsight.ingestion.support;

import com.claiminsight.ingestion.config.AppConfig;
import com.claiminsight.ingestion.config.SwaggerConfig;
import com.claiminsight.ingestion.config.ValidationConfig;
import com.claiminsight.ingestion.dto.DataFeedRequestDTO;
import com.claiminsight.ingestion.dto.DataFeedResponseDTO;
import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.mapper.ClaimRawMapper;
import com.claiminsight.ingestion.mapper.DataFeedMapper;
import com.claiminsight.ingestion.model.ClaimRaw;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.MessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComponentSupportTest {

    @Test
    void appConfig_providesStrictModelMapperAndExchangeRepository() {
        AppConfig appConfig = new AppConfig();

        ModelMapper modelMapper = appConfig.modelMapper();
        HttpExchangeRepository repository = appConfig.httpExchangeRepository();

        assertEquals(MatchingStrategies.STRICT, modelMapper.getConfiguration().getMatchingStrategy());
        assertInstanceOf(InMemoryHttpExchangeRepository.class, repository);
    }

    @Test
    void validationConfig_resolvesMessagesAndValidatesDtos() {
        ValidationConfig validationConfig = new ValidationConfig();

        MessageSource messageSource = validationConfig.messageSource();
        LocalValidatorFactoryBean validator = validationConfig.validator();
        validator.afterPropertiesSet();

        assertEquals(
                "feedType is required",
                messageSource.getMessage("NotBlank.dataFeedRequestDTO.feedType", null, Locale.ENGLISH)
        );

        DataFeedRequestDTO request = new DataFeedRequestDTO();
        request.setFeedType("");
        request.setSourceSystem("A");
        request.setStatus("123");

        Set<String> messages = validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(messages.contains("feedType is required"));
        assertTrue(messages.contains("sourceSystem must be between 2 and 100 characters"));
        assertTrue(messages.contains("status must contain letters only (e.g. ACTIVE, INACTIVE)"));
    }

    @Test
    void swaggerConfig_returnsExpectedMetadata() {
        SwaggerConfig swaggerConfig = new SwaggerConfig();

        OpenAPI openAPI = swaggerConfig.openAPI();

        assertEquals("Data Ingestion Service API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getInfo().getDescription().contains("Microservice 2"));
    }

    @Test
    void dataFeedMapper_mapsRequestAndResponse() {
        DataFeedMapper mapper = new DataFeedMapper(new AppConfig().modelMapper());

        DataFeedRequestDTO request = new DataFeedRequestDTO();
        request.setFeedType("CLAIM");
        request.setSourceSystem("ClaimsPro");
        request.setStatus("ACTIVE");

        DataFeed entity = mapper.toEntity(request, FeedType.CLAIM, FeedStatus.ACTIVE);
        assertEquals("ClaimsPro", entity.getSourceSystem());
        assertEquals(FeedType.CLAIM, entity.getFeedType());
        assertEquals(FeedStatus.ACTIVE, entity.getStatus());
        assertNull(entity.getFeedId());
        assertNull(entity.getCreatedDate());
        assertNull(entity.getLastSyncDate());

        entity.setFeedId(7L);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastSyncDate(LocalDateTime.now());

        DataFeedResponseDTO response = mapper.toResponseDTO(entity);
        assertEquals("CLAIM", response.getFeedType());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("ClaimsPro", response.getSourceSystem());
    }

    @Test
    void claimRawMapper_mapsRequestAndResponse() {
        ClaimRawMapper mapper = new ClaimRawMapper(new AppConfig().modelMapper());

        DataFeed feed = new DataFeed();
        feed.setFeedId(9L);
        feed.setFeedType(FeedType.PAYMENT);

        IngestionRequestDTO request = new IngestionRequestDTO();
        request.setClaimId("CLM-42");
        request.setFeedId(9L);
        request.setPayloadJson("{\"amount\":42}");

        ClaimRaw entity = mapper.toEntity(request, feed);
        assertEquals("CLM-42", entity.getClaimId());
        assertSame(feed, entity.getDataFeed());
        assertNull(entity.getRawId());
        assertNull(entity.getIngestedDate());

        entity.setRawId(5L);
        entity.setIngestedDate(LocalDateTime.now());

        IngestionResponseDTO response = mapper.toResponseDTO(entity);
        assertEquals(5L, response.getRawId());
        assertEquals(9L, response.getFeedId());
        assertEquals("PAYMENT", response.getFeedType());
        assertEquals("{\"amount\":42}", response.getPayloadJson());
    }
}
