package com.claim360.fraudrisk.controller;

import com.claim360.fraudrisk.dto.RiskIndicatorRequest;
import com.claim360.fraudrisk.dto.RiskIndicatorResponse;
import com.claim360.fraudrisk.enums.IndicatorType;
import com.claim360.fraudrisk.service.RiskIndicatorService;
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
@RequestMapping("/api/risk-indicators")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Risk Indicator",
        description = "APIs for managing Fraud Risk Indicators — " +
                "create, read, update, delete and filter operations"
)
public class RiskIndicatorController {

    private final RiskIndicatorService riskIndicatorService;

    // ── POST ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Create a new Risk Indicator",
            description = "Creates a new fraud risk indicator for a claim. " +
                    "ClaimId must follow pattern CLM-[digits]. " +
                    "Severity must be LOW, MEDIUM or HIGH."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Risk Indicator created successfully",
                    content = @Content(schema = @Schema(implementation = RiskIndicatorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid input",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<RiskIndicatorResponse> createRiskIndicator(
            @Valid @RequestBody RiskIndicatorRequest request) {
        log.info("REST request to create RiskIndicator for claimId: {}", request.getClaimId());
        RiskIndicatorResponse response = riskIndicatorService.createRiskIndicator(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ── GET All ──────────────────────────────────────────────────────────────
    @Operation(
            summary = "Get all Risk Indicators",
            description = "Retrieves a list of all fraud risk indicators in the system."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RiskIndicatorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<RiskIndicatorResponse>> getAllRiskIndicators() {
        log.info("REST request to get all RiskIndicators");
        return ResponseEntity.ok(riskIndicatorService.getAllRiskIndicators());
    }

    // ── GET by ID ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Get Risk Indicator by ID",
            description = "Retrieves a single fraud risk indicator by its unique ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk Indicator found",
                    content = @Content(schema = @Schema(implementation = RiskIndicatorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Risk Indicator not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<RiskIndicatorResponse> getRiskIndicatorById(
            @Parameter(description = "ID of the Risk Indicator to retrieve", required = true)
            @PathVariable Long id) {
        log.info("REST request to get RiskIndicator by ID: {}", id);
        return ResponseEntity.ok(riskIndicatorService.getRiskIndicatorById(id));
    }

    // ── GET by ClaimId ───────────────────────────────────────────────────────

    @Operation(
            summary = "Get Risk Indicators by Claim ID",
            description = "Retrieves all fraud risk indicators associated with a specific claim ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No indicators found for this claim"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<RiskIndicatorResponse>> getRiskIndicatorsByClaimId(
            @Parameter(description = "Claim ID to filter by (e.g. CLM-001)", required = true)
            @PathVariable String claimId) {
        log.info("REST request to get RiskIndicators by claimId: {}", claimId);
        return ResponseEntity.ok(riskIndicatorService.getRiskIndicatorsByClaimId(claimId));
    }

    // ── GET by Type ──────────────────────────────────────────────────────────
    @Operation(
            summary = "Get Risk Indicators by Type",
            description = "Retrieves all fraud risk indicators of a specific type. " +
                    "Allowed values: HighCost, UnusualTiming, Pattern."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No indicators found for this type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/type/{indicatorType}")
    public ResponseEntity<List<RiskIndicatorResponse>> getRiskIndicatorsByType(
            @Parameter(description = "Indicator type: HighCost, UnusualTiming, Pattern",
                    required = true)
            @PathVariable IndicatorType indicatorType) {
        log.info("REST request to get RiskIndicators by type: {}", indicatorType);
        return ResponseEntity.ok(riskIndicatorService.getRiskIndicatorsByType(indicatorType));
    }

    // ── GET by Severity ──────────────────────────────────────────────────────

    @Operation(
            summary = "Get Risk Indicators by Severity",
            description = "Retrieves all fraud risk indicators with a specific severity level. " +
                    "Allowed values: LOW, MEDIUM, HIGH."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No indicators found for this severity"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<RiskIndicatorResponse>> getRiskIndicatorsBySeverity(
            @Parameter(description = "Severity level: LOW, MEDIUM, HIGH", required = true)
            @PathVariable String severity) {
        log.info("REST request to get RiskIndicators by severity: {}", severity);
        return ResponseEntity.ok(riskIndicatorService.getRiskIndicatorsBySeverity(severity));
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Update a Risk Indicator",
            description = "Updates an existing fraud risk indicator by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk Indicator updated successfully",
                    content = @Content(schema = @Schema(implementation = RiskIndicatorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — invalid input"),
            @ApiResponse(responseCode = "404", description = "Risk Indicator not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RiskIndicatorResponse> updateRiskIndicator(
            @Parameter(description = "ID of the Risk Indicator to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody RiskIndicatorRequest request) {
        log.info("REST request to update RiskIndicator with ID: {}", id);
        return ResponseEntity.ok(riskIndicatorService.updateRiskIndicator(id, request));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Operation(
            summary = "Delete a Risk Indicator",
            description = "Deletes a fraud risk indicator by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Risk Indicator deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Risk Indicator not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRiskIndicator(
            @Parameter(description = "ID of the Risk Indicator to delete", required = true)
            @PathVariable Long id) {
        log.info("REST request to delete RiskIndicator with ID: {}", id);
        riskIndicatorService.deleteRiskIndicator(id);
        return ResponseEntity.ok("RiskIndicator with ID " + id + " deleted successfully.");
    }
}