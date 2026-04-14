package com.claiminsight.ingestion.controller;

import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST controller for ingesting and querying raw claim records. Base path: /api/ingest. */
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Claim Ingestion", description = "Ingest raw claim payloads and query stored records")
public class IngestionController {
    private final IngestionService ingestionService;
    
    /** Ingests a raw claim JSON payload and links it to a feed. Returns 201. */
    @PostMapping
    @Operation(summary = "Ingest a raw claim payload from an external system")
    public ResponseEntity<IngestionResponseDTO> ingestClaim(@Valid @RequestBody IngestionRequestDTO requestDTO) {
        log.info("POST /api/ingest — claimId: {}, feedId: {}", requestDTO.getClaimId(), requestDTO.getFeedId());
        IngestionResponseDTO response = ingestionService.ingestClaim(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /** Returns all ingested raw claim records. */
    @GetMapping("/raw-claims")
    @Operation(summary = "Get all ingested raw claim records")
    public ResponseEntity<List<IngestionResponseDTO>> getAllRawClaims() {
        log.info("GET /api/ingest/raw-claims");
        return ResponseEntity.ok(ingestionService.getAllRawClaims());
    }
    
    /** Returns raw claim records matching the given claimId. */
    @GetMapping("/raw-claims/{claimId}")
    @Operation(summary = "Get raw claim records by claim ID")
    public ResponseEntity<List<IngestionResponseDTO>> getRawClaimsByClaimId(@PathVariable String claimId) {
        log.info("GET /api/ingest/raw-claims/{}", claimId);
        return ResponseEntity.ok(ingestionService.getRawClaimsByClaimId(claimId));
    }
    
    /** Returns raw claim records belonging to the given feed. Returns 404 if feed not found. */
    @GetMapping("/raw-claims/feed/{feedId}")
    @Operation(summary = "Get raw claim records by the feed they belong to")
    public ResponseEntity<List<IngestionResponseDTO>> getRawClaimsByFeedId(@PathVariable Long feedId) {
        log.info("GET /api/ingest/raw-claims/feed/{}", feedId);
        return ResponseEntity.ok(ingestionService.getRawClaimsByFeedId(feedId));
    }
    
    /** Deletes a raw claim record by its ID. Returns 204 on success. */
    @DeleteMapping("/raw-claims/{rawId}")
    @Operation(summary = "Delete a raw claim record by its ID")
    public ResponseEntity<Void> deleteRawClaim(@PathVariable Long rawId) {
        log.info("DELETE /api/ingest/raw-claims/{}", rawId);
        ingestionService.deleteRawClaim(rawId);
        return ResponseEntity.noContent().build();
    }
}
