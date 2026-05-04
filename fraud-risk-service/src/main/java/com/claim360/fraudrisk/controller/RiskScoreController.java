package com.claim360.fraudrisk.controller;

import com.claim360.fraudrisk.dto.AutoEvaluateRequest;
import com.claim360.fraudrisk.dto.RiskScoreRequest;
import com.claim360.fraudrisk.dto.RiskScoreResponse;
import com.claim360.fraudrisk.service.RiskScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-scores")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Risk Score",
        description = "APIs for managing Fraud Risk Scores — " +
                "create, read, update, delete and filter operations"
)
public class RiskScoreController {

    private final RiskScoreService riskScoreService;

    // ── POST ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Create a new Risk Score",
            description = "Creates a new fraud risk score for a claim. " +
                    "ClaimId must follow pattern CLM-[digits]. " +
                    "Score value must be between 0.0 and 100.0."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Risk Score created successfully",
                    content = @Content(schema = @Schema(implementation = RiskScoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<RiskScoreResponse> createRiskScore(
            @Valid @RequestBody RiskScoreRequest request) {
        log.info("REST request to create RiskScore for claimId: {}", request.getClaimId());
        RiskScoreResponse response = riskScoreService.createRiskScore(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ── GET All ──────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get all Risk Scores",
            description = "Retrieves a list of all fraud risk scores in the system."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RiskScoreResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<RiskScoreResponse>> getAllRiskScores() {
        log.info("REST request to get all RiskScores");
        return ResponseEntity.ok(riskScoreService.getAllRiskScores());
    }

    // ── GET by ID ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get Risk Score by ID",
            description = "Retrieves a single fraud risk score by its unique ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk Score found",
                    content = @Content(schema = @Schema(implementation = RiskScoreResponse.class))),
            @ApiResponse(responseCode = "404", description = "Risk Score not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<RiskScoreResponse> getRiskScoreById(
            @Parameter(description = "ID of the Risk Score to retrieve", required = true)
            @PathVariable Long id) {
        log.info("REST request to get RiskScore by ID: {}", id);
        return ResponseEntity.ok(riskScoreService.getRiskScoreById(id));
    }

    // ── GET by ClaimId ───────────────────────────────────────────────────────

    @Operation(
            summary = "Get Risk Scores by Claim ID",
            description = "Retrieves all fraud risk scores associated with a specific claim ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No scores found for this claim"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<RiskScoreResponse>> getRiskScoresByClaimId(
            @Parameter(description = "Claim ID to filter by (e.g. CLM-001)", required = true)
            @PathVariable String claimId) {
        log.info("REST request to get RiskScores by claimId: {}", claimId);
        return ResponseEntity.ok(riskScoreService.getRiskScoresByClaimId(claimId));
    }

    // ── GET Latest ───────────────────────────────────────────────────────────

    @Operation(
            summary = "Get Latest Risk Score by Claim ID",
            description = "Retrieves the most recent fraud risk score for a specific claim ID " +
                    "based on computed date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest score retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No score found for this claim"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/claim/{claimId}/latest")
    public ResponseEntity<RiskScoreResponse> getLatestRiskScoreByClaimId(
            @Parameter(description = "Claim ID to get latest score for (e.g. CLM-001)",
                    required = true)
            @PathVariable String claimId) {
        log.info("REST request to get latest RiskScore for claimId: {}", claimId);
        return ResponseEntity.ok(riskScoreService.getLatestRiskScoreByClaimId(claimId));
    }

    // ── GET by Threshold ─────────────────────────────────────────────────────

    @Operation(
            summary = "Get Risk Scores above Threshold",
            description = "Retrieves all fraud risk scores with score value " +
                    "greater than or equal to the given threshold."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No scores found above threshold"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/threshold/{threshold}")
    public ResponseEntity<List<RiskScoreResponse>> getRiskScoresAboveThreshold(
            @Parameter(description = "Minimum score value threshold (e.g. 80.0)", required = true)
            @PathVariable Double threshold) {
        log.info("REST request to get RiskScores above threshold: {}", threshold);
        return ResponseEntity.ok(riskScoreService.getRiskScoresAboveThreshold(threshold));
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Update a Risk Score",
            description = "Updates an existing fraud risk score by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk Score updated successfully",
                    content = @Content(schema = @Schema(implementation = RiskScoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid input"),
            @ApiResponse(responseCode = "404", description = "Risk Score not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RiskScoreResponse> updateRiskScore(
            @Parameter(description = "ID of the Risk Score to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RiskScoreRequest request) {
        log.info("REST request to update RiskScore with ID: {}", id);
        return ResponseEntity.ok(riskScoreService.updateRiskScore(id, request));
    }

    // ── Auto-Evaluate (data-ingestion-service pipeline) ──────────────────────

    @Operation(
            summary = "Auto-evaluate fraud risk from ingestion payload",
            description = "Called by data-ingestion-service after a claim is ingested. " +
                    "Applies rule-based indicators (HighCost, UnusualTiming) to the raw " +
                    "JSON payload and returns a computed risk score."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Risk score computed and saved",
                    content = @Content(schema = @Schema(implementation = RiskScoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/auto-evaluate")
    public ResponseEntity<RiskScoreResponse> autoEvaluate(
            @RequestBody AutoEvaluateRequest request) {
        log.info("REST auto-evaluate fraud risk for claimId: {}", request.getClaimId());
        RiskScoreResponse response = riskScoreService.autoEvaluateRisk(
                request.getClaimId(), request.getPayloadJson());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Operation(
            summary = "Delete a Risk Score",
            description = "Deletes a fraud risk score by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk Score deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Risk Score not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRiskScore(
            @Parameter(description = "ID of the Risk Score to delete", required = true)
            @PathVariable Long id) {
        log.info("REST request to delete RiskScore with ID: {}", id);
        riskScoreService.deleteRiskScore(id);
        return ResponseEntity.ok("RiskScore with ID " + id + " deleted successfully.");
    }
}