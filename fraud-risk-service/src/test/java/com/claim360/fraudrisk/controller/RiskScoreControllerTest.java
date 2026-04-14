package com.claim360.fraudrisk.controller;

import com.claim360.fraudrisk.dto.RiskScoreRequest;
import com.claim360.fraudrisk.dto.RiskScoreResponse;
import com.claim360.fraudrisk.exception.GlobalExceptionHandler;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.service.RiskScoreService;
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

@WebMvcTest(RiskScoreController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RiskScore Controller Tests")
class RiskScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskScoreService riskScoreService;

    private ObjectMapper objectMapper;
    private RiskScoreRequest request;
    private RiskScoreResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new RiskScoreRequest();
        request.setClaimId("CLM-001");
        request.setScoreValue(85.5);
        request.setComputedDate(LocalDate.of(2024, 1, 15));

        response = new RiskScoreResponse(
                1L, "CLM-001", 85.5,
                LocalDate.of(2024, 1, 15)
        );
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST - Should create RiskScore and return 201")
    void createRiskScore_ShouldReturn201() throws Exception {
        when(riskScoreService.createRiskScore(any(RiskScoreRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/risk-scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.scoreValue").value(85.5));
    }

    @Test
    @DisplayName("POST - Should return 400 when fields are missing")
    void createRiskScore_WithMissingFields_ShouldReturn400() throws Exception {
        RiskScoreRequest invalidRequest = new RiskScoreRequest();

        mockMvc.perform(post("/api/risk-scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST - Should return 400 when claimId format is invalid")
    void createRiskScore_WithInvalidClaimId_ShouldReturn400() throws Exception {
        request.setClaimId("INVALID");

        mockMvc.perform(post("/api/risk-scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.claimId").exists());
    }

    @Test
    @DisplayName("POST - Should return 400 when scoreValue is out of range")
    void createRiskScore_WithInvalidScoreValue_ShouldReturn400() throws Exception {
        request.setScoreValue(150.0);

        mockMvc.perform(post("/api/risk-scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.scoreValue").exists());
    }

    // ── GET All ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return all RiskScores with 200")
    void getAllRiskScores_ShouldReturn200() throws Exception {
        when(riskScoreService.getAllRiskScores()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    // ── GET by ID ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskScore by ID with 200")
    void getRiskScoreById_WhenExists_ShouldReturn200() throws Exception {
        when(riskScoreService.getRiskScoreById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/risk-scores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    @DisplayName("GET - Should return 404 when RiskScore not found")
    void getRiskScoreById_WhenNotExists_ShouldReturn404() throws Exception {
        when(riskScoreService.getRiskScoreById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "RiskScore", "scoreId", 99L));

        mockMvc.perform(get("/api/risk-scores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    // ── GET by ClaimId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskScores by claimId with 200")
    void getRiskScoresByClaimId_WhenExists_ShouldReturn200() throws Exception {
        when(riskScoreService.getRiskScoresByClaimId("CLM-001"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-scores/claim/CLM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    // ── GET Latest ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return latest RiskScore for claimId")
    void getLatestRiskScoreByClaimId_WhenExists_ShouldReturn200() throws Exception {
        when(riskScoreService.getLatestRiskScoreByClaimId("CLM-001"))
                .thenReturn(response);

        mockMvc.perform(get("/api/risk-scores/claim/CLM-001/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.scoreValue").value(85.5));
    }

    // ── GET by Threshold ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET - Should return RiskScores above threshold")
    void getRiskScoresAboveThreshold_WhenExists_ShouldReturn200() throws Exception {
        when(riskScoreService.getRiskScoresAboveThreshold(80.0))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk-scores/threshold/80.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].scoreValue").value(85.5));
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT - Should update RiskScore and return 200")
    void updateRiskScore_WhenExists_ShouldReturn200() throws Exception {
        when(riskScoreService.updateRiskScore(eq(1L), any(RiskScoreRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/risk-scores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    @DisplayName("PUT - Should return 404 when updating non-existing RiskScore")
    void updateRiskScore_WhenNotExists_ShouldReturn404() throws Exception {
        when(riskScoreService.updateRiskScore(eq(99L), any(RiskScoreRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "RiskScore", "scoreId", 99L));

        mockMvc.perform(put("/api/risk-scores/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE - Should delete RiskScore and return 200")
    void deleteRiskScore_WhenExists_ShouldReturn200() throws Exception {
        doNothing().when(riskScoreService).deleteRiskScore(1L);

        mockMvc.perform(delete("/api/risk-scores/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "RiskScore with ID 1 deleted successfully."));
    }

    @Test
    @DisplayName("DELETE - Should return 404 when deleting non-existing RiskScore")
    void deleteRiskScore_WhenNotExists_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("RiskScore", "scoreId", 99L))
                .when(riskScoreService).deleteRiskScore(99L);

        mockMvc.perform(delete("/api/risk-scores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}