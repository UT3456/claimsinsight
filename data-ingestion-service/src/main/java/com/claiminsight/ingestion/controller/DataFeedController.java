package com.claiminsight.ingestion.controller;

import com.claiminsight.ingestion.dto.DataFeedRequestDTO;
import com.claiminsight.ingestion.dto.DataFeedResponseDTO;
import com.claiminsight.ingestion.dto.FeedStatusUpdateDTO;
import com.claiminsight.ingestion.service.DataFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST controller for managing data feed sources. Base path: /api/feeds. */
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Feed Management", description = "Register and manage external data feed sources")
public class DataFeedController {
    private final DataFeedService dataFeedService;
    
    /** Registers a new data feed source. Returns 201 on success. */
    @PostMapping
    @Operation(summary = "Register a new data feed source")
    public ResponseEntity<DataFeedResponseDTO> createFeed(@Valid @RequestBody DataFeedRequestDTO requestDTO) {
        log.info("POST /api/feeds — source: {}", requestDTO.getSourceSystem());
        DataFeedResponseDTO response = dataFeedService.createFeed(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /** Returns all registered data feeds. */
    @GetMapping
    @Operation(summary = "Get all registered data feeds")
    public ResponseEntity<List<DataFeedResponseDTO>> getAllFeeds() {
        log.info("GET /api/feeds");
        return ResponseEntity.ok(dataFeedService.getAllFeeds());
    }
    
    /** Returns a single data feed by its ID. Returns 404 if not found. */
    @GetMapping("/{feedId}")
    @Operation(summary = "Get a data feed by its ID")
    public ResponseEntity<DataFeedResponseDTO> getFeedById(@PathVariable Long feedId) {
        log.info("GET /api/feeds/{}", feedId);
        return ResponseEntity.ok(dataFeedService.getFeedById(feedId));
    }
    
    /** Updates the status of a data feed. Returns 404 if not found. */
    @PutMapping("/{feedId}/status")
    @Operation(summary = "Update the status of a data feed")
    public ResponseEntity<DataFeedResponseDTO> updateStatus(
            @PathVariable Long feedId,
            @Valid @RequestBody FeedStatusUpdateDTO updateDTO) {
        log.info("PUT /api/feeds/{}/status — new status: {}", feedId, updateDTO.getStatus());
        return ResponseEntity.ok(dataFeedService.updateStatus(feedId, updateDTO));
    }
    
    /** Deletes a data feed by its ID. Returns 204 on success. */
    @DeleteMapping("/{feedId}")
    @Operation(summary = "Delete a data feed by its ID")
    public ResponseEntity<Void> deleteFeed(@PathVariable Long feedId) {
        log.info("DELETE /api/feeds/{}", feedId);
        dataFeedService.deleteFeed(feedId);
        return ResponseEntity.noContent().build();
    }
}
