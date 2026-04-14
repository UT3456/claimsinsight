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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/** Service layer for data feed management. Results are cached in the 'feeds' cache. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataFeedService {

    private final DataFeedRepository dataFeedRepository;
    private final DataFeedMapper dataFeedMapper;

    /** Creates and saves a new DataFeed. Clears the feeds cache. */
    @CacheEvict(value = "feeds", allEntries = true)
    public DataFeedResponseDTO createFeed(DataFeedRequestDTO request) {
        FeedType type     = parseFeedType(request.getFeedType());
        FeedStatus status = parseFeedStatus(request.getStatus());
        DataFeed saved    = dataFeedRepository.save(dataFeedMapper.toEntity(request, type, status));
        log.info("DataFeed created with ID: {}", saved.getFeedId());
        return dataFeedMapper.toResponseDTO(saved);
    }

    /** Returns all DataFeeds. Result is cached. */
    @Cacheable(value = "feeds", key = "'all'")
    public List<DataFeedResponseDTO> getAllFeeds() {
        return dataFeedRepository.findAll()
                .stream()
                .map(dataFeedMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /** Returns a DataFeed by ID. Result is cached. Throws 404 if not found. */
    @Cacheable(value = "feeds", key = "#feedId")
    public DataFeedResponseDTO getFeedById(Long feedId) {
        return dataFeedMapper.toResponseDTO(findOrThrow(feedId));
    }

    /** Updates the status of a DataFeed. Clears the feeds cache. */
    @CacheEvict(value = "feeds", allEntries = true)
    public DataFeedResponseDTO updateStatus(Long feedId, FeedStatusUpdateDTO request) {
        DataFeed feed = findOrThrow(feedId);
        feed.setStatus(parseFeedStatus(request.getStatus()));
        return dataFeedMapper.toResponseDTO(dataFeedRepository.save(feed));
    }

    /** Deletes a DataFeed by ID. Clears the feeds cache. Throws 404 if not found. */
    @CacheEvict(value = "feeds", allEntries = true)
    public void deleteFeed(Long feedId) {
        findOrThrow(feedId);
        dataFeedRepository.deleteById(feedId);
        log.info("DataFeed {} deleted", feedId);
    }

    private DataFeed findOrThrow(Long feedId) {
        return dataFeedRepository.findById(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("DataFeed with ID " + feedId + " not found"));
    }

    private FeedType parseFeedType(String value) {
        try {
            return FeedType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidFeedTypeException("Invalid feedType: '" + value + "'. Allowed: CLAIM, POLICY, PAYMENT, RESERVE");
        }
    }

    private FeedStatus parseFeedStatus(String value) {
        try {
            return FeedStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidFeedTypeException("Invalid status: '" + value + "'. Allowed: ACTIVE, INACTIVE, FAILED");
        }
    }
}
