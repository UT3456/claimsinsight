package com.claiminsight.ingestion.service;

import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.exception.InvalidFeedStatusException;
import com.claiminsight.ingestion.exception.ResourceNotFoundException;
import com.claiminsight.ingestion.mapper.ClaimRawMapper;
import com.claiminsight.ingestion.model.ClaimRaw;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
import com.claiminsight.ingestion.repository.ClaimRawRepository;
import com.claiminsight.ingestion.repository.DataFeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock ClaimRawRepository claimRawRepository;
    @Mock DataFeedRepository dataFeedRepository;
    @Mock ClaimRawMapper claimRawMapper;
    @InjectMocks IngestionService ingestionService;

    private DataFeed feed;
    private ClaimRaw raw;
    private IngestionResponseDTO dto;

    @BeforeEach
    void setUp() {
        feed = new DataFeed();
        feed.setFeedId(1L);
        feed.setFeedType(FeedType.CLAIM);
        feed.setSourceSystem("ClaimsPro v3");
        feed.setStatus(FeedStatus.ACTIVE);
        feed.setCreatedDate(LocalDateTime.now());

        raw = new ClaimRaw();
        raw.setRawId(1L);
        raw.setClaimId("CLM-001");
        raw.setDataFeed(feed);
        raw.setPayloadJson("{\"amount\":5000}");
        raw.setIngestedDate(LocalDateTime.now());

        dto = new IngestionResponseDTO();
        dto.setRawId(1L);
        dto.setClaimId("CLM-001");
        dto.setFeedId(1L);
    }

    @Test
    void ingestClaim_success() {
        IngestionRequestDTO req = new IngestionRequestDTO();
        req.setClaimId("CLM-001");
        req.setFeedId(1L);
        req.setPayloadJson("{\"amount\":5000}");

        when(dataFeedRepository.findById(1L)).thenReturn(Optional.of(feed));
        when(claimRawMapper.toEntity(any(), any())).thenReturn(raw);
        when(claimRawRepository.save(any())).thenReturn(raw);
        when(dataFeedRepository.save(any())).thenReturn(feed);
        when(claimRawMapper.toResponseDTO(any())).thenReturn(dto);

        assertNotNull(ingestionService.ingestClaim(req));
    }

    @Test
    void ingestClaim_feedNotFound_throwsException() {
        IngestionRequestDTO req = new IngestionRequestDTO();
        req.setClaimId("CLM-001");
        req.setFeedId(99L);
        req.setPayloadJson("{}");

        when(dataFeedRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ingestionService.ingestClaim(req));
        verify(claimRawRepository, never()).save(any());
    }

    @Test
    void ingestClaim_inactiveFeed_throwsException() {
        IngestionRequestDTO req = new IngestionRequestDTO();
        req.setClaimId("CLM-001");
        req.setFeedId(1L);
        req.setPayloadJson("{}");
        feed.setStatus(FeedStatus.FAILED);

        when(dataFeedRepository.findById(1L)).thenReturn(Optional.of(feed));

        assertThrows(InvalidFeedStatusException.class, () -> ingestionService.ingestClaim(req));
        verify(claimRawRepository, never()).save(any());
    }

    @Test
    void getAllRawClaims_returnsList() {
        when(claimRawRepository.findAll()).thenReturn(List.of(raw));
        when(claimRawMapper.toResponseDTO(raw)).thenReturn(dto);

        assertEquals(1, ingestionService.getAllRawClaims().size());
    }

    @Test
    void getRawClaimsByClaimId_returnsList() {
        when(claimRawRepository.findByClaimId("CLM-001")).thenReturn(List.of(raw));
        when(claimRawMapper.toResponseDTO(raw)).thenReturn(dto);

        assertEquals(1, ingestionService.getRawClaimsByClaimId("CLM-001").size());
    }

    @Test
    void getRawClaimsByFeedId_feedNotFound_throwsException() {
        when(dataFeedRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> ingestionService.getRawClaimsByFeedId(99L));
    }

    @Test
    void getRawClaimsByFeedId_returnsList() {
        when(dataFeedRepository.existsById(1L)).thenReturn(true);
        when(claimRawRepository.findByDataFeed_FeedId(1L)).thenReturn(List.of(raw));
        when(claimRawMapper.toResponseDTO(raw)).thenReturn(dto);

        assertEquals(1, ingestionService.getRawClaimsByFeedId(1L).size());
    }

    @Test
    void deleteRawClaim_success() {
        when(claimRawRepository.existsById(1L)).thenReturn(true);

        ingestionService.deleteRawClaim(1L);

        verify(claimRawRepository).deleteById(1L);
    }

    @Test
    void deleteRawClaim_notFound_throwsException() {
        when(claimRawRepository.existsById(9L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> ingestionService.deleteRawClaim(9L));
        verify(claimRawRepository, never()).deleteById(any());
    }
}
