package com.claim360.fraudrisk.controller;

import com.claim360.fraudrisk.dto.RiskIndicatorRequest;
import com.claim360.fraudrisk.dto.RiskIndicatorResponse;
import com.claim360.fraudrisk.enums.IndicatorType;
import com.claim360.fraudrisk.exception.GlobalExceptionHandler;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.service.RiskIndicatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskIndicatorController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RiskIndicator Controller Tests")
class RiskIndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskIndicatorService riskIndicatorService;

    private ObjectMapper objectMapper;
    private RiskIndicatorRequest request;
    private RiskIndicatorResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new RiskIndicatorRequest();
        request.setClaimId("CLM-001");
        request.setIndicatorType(IndicatorType.HighCost);
        request.setSeverity("HIGH");
        request.setTriggeredDate(LocalDate.of(2024, 1, 15));

        response = new RiskIndicatorResponse(
                1L, "CLM-001", IndicatorType.HighCost,
                "HIGH", LocalDate.of(2024, 1, 15)
        );
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST - Should create RiskIndicator and return 201")
    void createRiskIndicator_ShouldReturn201() throws Exception {
        when(riskIndicatorService.createRiskIndicator(any(RiskIndicatorRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/risk-indicators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.indicatorId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.indicatorType").value("HighCost"));
    }

    @Test
    @DisplayName("POST - Should return 400 when fields are missing")
    void createRiskIndicator_WithMissingFields_ShouldReturn400() throws Exception {
        RiskIndicatorRequest invalidRequest = new RiskIndicatorRequest();

        mockMvc.perform(post("/api/risk-indicators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST - Should return 400 when claimId format is invalid")
    void createRiskIndicator_WithInvalidClaimId_ShouldReturn400() throws Exception {
        request.setClaimId("INVALID-ID");

        mockMvc.perform(post("/api/risk-indicators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.claimId").exists());
    }

    @Test
    @DisplayName("POST - Should return 400 when severity is invalid")
    void createRiskIndicator_WithInvalidSeverity_ShouldReturn400() throws Exception {
        request.setSeverity("CRITICAL");

        mockMvc.perform(post("/api/risk-indicators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.severity").exists());
    }

    // ── GET All ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return all RiskIndicators with 200")
    void getAllRiskIndicators_ShouldReturn200() throws Exception {
        when(riskIndicatorService.getAllRiskIndicators()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    // ── GET by ID ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskIndicator by ID with 200")
    void getRiskIndicatorById_WhenExists_ShouldReturn200() throws Exception {
        when(riskIndicatorService.getRiskIndicatorById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/risk-indicators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicatorId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    @DisplayName("GET - Should return 404 when RiskIndicator not found")
    void getRiskIndicatorById_WhenNotExists_ShouldReturn404() throws Exception {
        when(riskIndicatorService.getRiskIndicatorById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "RiskIndicator", "indicatorId", 99L));

        mockMvc.perform(get("/api/risk-indicators/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    // ── GET by ClaimId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskIndicators by claimId with 200")
    void getRiskIndicatorsByClaimId_WhenExists_ShouldReturn200() throws Exception {
        when(riskIndicatorService.getRiskIndicatorsByClaimId("CLM-001"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-indicators/claim/CLM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    // ── GET by Type ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskIndicators by type with 200")
    void getRiskIndicatorsByType_WhenExists_ShouldReturn200() throws Exception {
        when(riskIndicatorService.getRiskIndicatorsByType(IndicatorType.HighCost))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-indicators/type/HighCost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].indicatorType").value("HighCost"));
    }

    // ── GET by Severity ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskIndicators by severity with 200")
    void getRiskIndicatorsBySeverity_WhenExists_ShouldReturn200() throws Exception {
        when(riskIndicatorService.getRiskIndicatorsBySeverity("HIGH"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-indicators/severity/HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT - Should update RiskIndicator and return 200")
    void updateRiskIndicator_WhenExists_ShouldReturn200() throws Exception {
        when(riskIndicatorService.updateRiskIndicator(
                eq(1L), any(RiskIndicatorRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/risk-indicators/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicatorId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    @DisplayName("PUT - Should return 404 when updating non-existing RiskIndicator")
    void updateRiskIndicator_WhenNotExists_ShouldReturn404() throws Exception {
        when(riskIndicatorService.updateRiskIndicator(
                eq(99L), any(RiskIndicatorRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "RiskIndicator", "indicatorId", 99L));

        mockMvc.perform(put("/api/risk-indicators/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE - Should delete RiskIndicator and return 200")
    void deleteRiskIndicator_WhenExists_ShouldReturn200() throws Exception {
        doNothing().when(riskIndicatorService).deleteRiskIndicator(1L);

        mockMvc.perform(delete("/api/risk-indicators/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "RiskIndicator with ID 1 deleted successfully."));
    }

    @Test
    @DisplayName("DELETE - Should return 404 when deleting non-existing RiskIndicator")
    void deleteRiskIndicator_WhenNotExists_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException(
                "RiskIndicator", "indicatorId", 99L))
                .when(riskIndicatorService).deleteRiskIndicator(99L);

        mockMvc.perform(delete("/api/risk-indicators/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}