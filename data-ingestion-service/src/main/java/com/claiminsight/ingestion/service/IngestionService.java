package com.claiminsight.ingestion.service;

import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.exception.InvalidFeedStatusException;
import com.claiminsight.ingestion.exception.ResourceNotFoundException;
import com.claiminsight.ingestion.mapper.ClaimRawMapper;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.repository.ClaimRawRepository;
import com.claiminsight.ingestion.repository.DataFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Service layer for ingesting and querying raw claim records. Results are cached in 'rawClaims'. */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final ClaimRawRepository claimRawRepository;
    private final DataFeedRepository dataFeedRepository;
    private final ClaimRawMapper claimRawMapper;

    /** Ingests a raw claim, links it to a DataFeed, and updates the feed's last sync date. */
    @CacheEvict(value = {"rawClaims", "feeds"}, allEntries = true)
    public IngestionResponseDTO ingestClaim(IngestionRequestDTO request) {
        DataFeed feed = dataFeedRepository.findById(request.getFeedId())
                .orElseThrow(() -> new ResourceNotFoundException("DataFeed with ID " + request.getFeedId() + " not found"));

        // Business rule: only ACTIVE feeds can receive new claim records
        if (feed.getStatus() != FeedStatus.ACTIVE) {
            throw new InvalidFeedStatusException(
                    "Cannot ingest claim into feed ID " + request.getFeedId() +
                    " — feed status is " + feed.getStatus() + ". Only ACTIVE feeds are allowed.");
        }

        var saved = claimRawRepository.save(claimRawMapper.toEntity(request, feed));

        feed.setLastSyncDate(LocalDateTime.now());
        dataFeedRepository.save(feed);

        log.info("Claim {} ingested, rawId: {}", request.getClaimId(), saved.getRawId());
        return claimRawMapper.toResponseDTO(saved);
    }

    /** Returns all raw claim records. Result is cached. */
    @Cacheable(value = "rawClaims", key = "'all'")
    public List<IngestionResponseDTO> getAllRawClaims() {
        return claimRawRepository.findAll()
                .stream().map(claimRawMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns raw records by claimId. Result is cached. */
    @Cacheable(value = "rawClaims", key = "#claimId")
    public List<IngestionResponseDTO> getRawClaimsByClaimId(String claimId) {
        return claimRawRepository.findByClaimId(claimId)
                .stream().map(claimRawMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Returns raw records by feedId. Throws 404 if feed does not exist. */
    @Cacheable(value = "rawClaims", key = "'feed-' + #feedId")
    public List<IngestionResponseDTO> getRawClaimsByFeedId(Long feedId) {
        if (!dataFeedRepository.existsById(feedId)) {
            throw new ResourceNotFoundException("DataFeed with ID " + feedId + " not found");
        }
        return claimRawRepository.findByDataFeed_FeedId(feedId)
                .stream().map(claimRawMapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Deletes a raw claim record by ID. Throws 404 if not found. */
    @CacheEvict(value = "rawClaims", allEntries = true)
    public void deleteRawClaim(Long rawId) {
        if (!claimRawRepository.existsById(rawId)) {
            throw new ResourceNotFoundException("ClaimRaw with ID " + rawId + " not found");
        }
        claimRawRepository.deleteById(rawId);
    }
}
