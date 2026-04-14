package com.claiminsight.ingestion.service;

import com.claiminsight.ingestion.dto.DataFeedRequestDTO;
import com.claiminsight.ingestion.dto.DataFeedResponseDTO;
import com.claiminsight.ingestion.dto.FeedStatusUpdateDTO;
import com.claiminsight.ingestion.exception.InvalidFeedTypeException;
import com.claiminsight.ingestion.exception.ResourceNotFoundException;
import com.claiminsight.ingestion.mapper.DataFeedMapper;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
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
class DataFeedServiceTest {

    @Mock DataFeedRepository dataFeedRepository;
    @Mock DataFeedMapper dataFeedMapper;
    @InjectMocks DataFeedService dataFeedService;

    private DataFeed feed;
    private DataFeedResponseDTO dto;

    @BeforeEach
    void setUp() {
        feed = new DataFeed();
        feed.setFeedId(1L);
        feed.setFeedType(FeedType.CLAIM);
        feed.setSourceSystem("ClaimsPro v3");
        feed.setStatus(FeedStatus.ACTIVE);
        feed.setCreatedDate(LocalDateTime.now());

        dto = new DataFeedResponseDTO();
        dto.setFeedId(1L);
        dto.setFeedType("CLAIM");
        dto.setSourceSystem("ClaimsPro v3");
        dto.setStatus("ACTIVE");
    }

    @Test
    void createFeed_success() {
        DataFeedRequestDTO req = new DataFeedRequestDTO();
        req.setFeedType("CLAIM");
        req.setSourceSystem("ClaimsPro v3");
        req.setStatus("ACTIVE");

        when(dataFeedMapper.toEntity(any(), any(), any())).thenReturn(feed);
        when(dataFeedRepository.save(any())).thenReturn(feed);
        when(dataFeedMapper.toResponseDTO(any())).thenReturn(dto);

        DataFeedResponseDTO result = dataFeedService.createFeed(req);

        assertNotNull(result);
        assertEquals("CLAIM", result.getFeedType());
    }

    @Test
    void createFeed_invalidFeedType_throwsException() {
        DataFeedRequestDTO req = new DataFeedRequestDTO();
        req.setFeedType("INVALID");
        req.setSourceSystem("System");
        req.setStatus("ACTIVE");

        assertThrows(InvalidFeedTypeException.class, () -> dataFeedService.createFeed(req));
        verify(dataFeedRepository, never()).save(any());
    }

    @Test
    void getAllFeeds_returnsList() {
        when(dataFeedRepository.findAll()).thenReturn(List.of(feed));
        when(dataFeedMapper.toResponseDTO(feed)).thenReturn(dto);

        assertEquals(1, dataFeedService.getAllFeeds().size());
    }

    @Test
    void getFeedById_found() {
        when(dataFeedRepository.findById(1L)).thenReturn(Optional.of(feed));
        when(dataFeedMapper.toResponseDTO(feed)).thenReturn(dto);

        assertNotNull(dataFeedService.getFeedById(1L));
    }

    @Test
    void getFeedById_notFound_throwsException() {
        when(dataFeedRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dataFeedService.getFeedById(99L));
    }

    @Test
    void updateStatus_success() {
        FeedStatusUpdateDTO request = new FeedStatusUpdateDTO();
        request.setStatus("FAILED");

        when(dataFeedRepository.findById(1L)).thenReturn(Optional.of(feed));
        when(dataFeedRepository.save(feed)).thenReturn(feed);
        when(dataFeedMapper.toResponseDTO(feed)).thenReturn(dto);

        DataFeedResponseDTO result = dataFeedService.updateStatus(1L, request);

        assertNotNull(result);
        assertEquals(FeedStatus.FAILED, feed.getStatus());
        verify(dataFeedRepository).save(feed);
    }

    @Test
    void updateStatus_invalidStatus_throwsException() {
        FeedStatusUpdateDTO request = new FeedStatusUpdateDTO();
        request.setStatus("UNKNOWN");

        when(dataFeedRepository.findById(1L)).thenReturn(Optional.of(feed));

        assertThrows(InvalidFeedTypeException.class, () -> dataFeedService.updateStatus(1L, request));
        verify(dataFeedRepository, never()).save(any());
    }

    @Test
    void deleteFeed_success() {
        when(dataFeedRepository.findById(1L)).thenReturn(Optional.of(feed));

        dataFeedService.deleteFeed(1L);

        verify(dataFeedRepository).deleteById(1L);
    }

    @Test
    void deleteFeed_notFound_throwsException() {
        when(dataFeedRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dataFeedService.deleteFeed(99L));
    }
}
