package com.demo.controllertesting;

import com.demo.controller.AdjusterPerformanceController;
import com.demo.dto.AdjusterPerformanceDTO;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.exception.InvalidInputException;
import com.demo.exception.ResourceNotFoundException;
import com.demo.service.AdjusterPerformanceService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdjusterPerformanceController.
 */
@ExtendWith(MockitoExtension.class)
public class AdjusterPerformanceControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdjusterPerformanceService service;

    @InjectMocks
    private AdjusterPerformanceController controller;

    private AdjusterPerformanceDTO dto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        dto = new AdjusterPerformanceDTO();
        dto.setPerfId(1L);
        dto.setAdjusterId(101L);
        dto.setClaimsHandled(25);
        dto.setTotalDaysTaken(100);
        dto.setPeriod("2026-Q1");
        dto.setAvgTat(4.0);
        dto.setQualityScore(88.0);
        dto.setProductivityFlag("NORMAL");
        dto.setPerformanceFlag("GOOD");
    }

    // ── POST /api/adjusters/performance ──────────────────────────────────

    @Test
    void test_savePerformance_positive() throws Exception {
        when(service.savePerformance(any(AdjusterPerformanceDTO.class))).thenReturn(dto);
        mockMvc.perform(post("/api/adjusters/performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void test_savePerformance_negative() throws Exception {
        when(service.savePerformance(any(AdjusterPerformanceDTO.class))).thenReturn(null);
        mockMvc.perform(post("/api/adjusters/performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Performance Resource Creation Failed"));
    }

    // ── GET /api/adjusters/{id}/performance ──────────────────────────────

    @Test
    void test_getPerformance_positive() throws Exception {
        when(service.getAdjusterPerformance(101L, "2026-Q1")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/adjusters/101/performance")
                        .param("period", "2026-Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjusterId").value(101));
    }

    @Test
    void test_getPerformance_negative() throws Exception {
        when(service.getAdjusterPerformance(999L, "2026-Q1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/adjusters/999/performance")
                        .param("period", "2026-Q1"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/adjusters/performance ───────────────────────────────────

    @Test
    void test_getAllPerformance_positive() throws Exception {
        when(service.listAllAdjusterPerformance("2026-Q1")).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/api/adjusters/performance")
                        .param("period", "2026-Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void test_getAllPerformance_negative() throws Exception {
        when(service.listAllAdjusterPerformance("2026-Q1")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/adjusters/performance")
                        .param("period", "2026-Q1"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/adjusters/top-performers ────────────────────────────────

    @Test
    void test_getTopPerformers_positive() throws Exception {
        when(service.getTopPerformers("2026-Q1")).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/api/adjusters/top-performers")
                        .param("period", "2026-Q1"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getTopPerformers_negative() throws Exception {
        when(service.getTopPerformers("2026-Q1")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/adjusters/top-performers")
                        .param("period", "2026-Q1"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/adjusters/training-flagged ──────────────────────────────

    @Test
    void test_getTrainingFlagged_positive() throws Exception {
        when(service.getAdjustersFlaggedForTraining("2026-Q1")).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/api/adjusters/training-flagged")
                        .param("period", "2026-Q1"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getTrainingFlagged_notFound_returns404() throws Exception {
//        when(service.getLowProductivityAdjusters(anyInt(),anyString())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/adjusters/training-flagged")
                        .param("period", "2026-Q5"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/adjusters/low-productivity ──────────────────────────────

    @Test
    void test_getLowProductivity_positive() throws Exception {
        when(service.getLowProductivityAdjusters(20, "2026-Q1")).thenReturn(Arrays.asList(dto));

        mockMvc.perform(get("/api/adjusters/low-productivity")
                        .param("period", "2026-Q1")
                        .param("threshold", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getLowProductivity_negative() throws Exception {
        when(service.getLowProductivityAdjusters(anyInt(),anyString())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/adjusters/low-productivity")
                        .param("period", "2026-Q5"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/adjusters/overloaded ─────────────────────────────────────

    @Test
    void test_getOverloaded_positive() throws Exception {
        List<AdjusterPerformanceDTO> list = Arrays.asList(dto);
        when(service.getOverloadedAdjusters(40, "2026-Q1")).thenReturn(list);

        mockMvc.perform(get("/api/adjusters/overloaded")
                        .param("period", "2026-Q1")
                        .param("threshold", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adjusterId").value(101));
    }

    @Test
    void test_getOverloaded_negative() throws Exception {
        when(service.getOverloadedAdjusters(40, "2026-Q1")).thenReturn(new java.util.ArrayList<>());

        mockMvc.perform(get("/api/adjusters/overloaded")
                        .param("period", "2026-Q1")
                        .param("threshold", "40"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/adjusters/slow-performers ───────────────────────────────

    @Test
    void test_getSlowPerformers_positive() throws Exception {
        List<AdjusterPerformanceDTO> list = Arrays.asList(dto);
        when(service.getSlowPerformers(5.0, "2026-Q1")).thenReturn(list);

        mockMvc.perform(get("/api/adjusters/slow-performers")
                        .param("period", "2026-Q1")
                        .param("tatTarget", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].avgTat").value(4.0));
    }

    @Test
    void test_getSlowPerformers_negative() throws Exception {
        when(service.getSlowPerformers(5.0, "2026-Q1")).thenReturn(new java.util.ArrayList<>());

        mockMvc.perform(get("/api/adjusters/slow-performers")
                        .param("period", "2026-Q1")
                        .param("tatTarget", "5.0"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/adjusters/high-denial-rate ──────────────────────────────

    @Test
    void test_getHighDenialRate_positive() throws Exception {
        List<AdjusterPerformanceDTO> list = Arrays.asList(dto);
        when(service.getAdjustersWithHighDenialRate(15.0, "2026-Q1")).thenReturn(list);

        mockMvc.perform(get("/api/adjusters/high-denial-rate")
                        .param("period", "2026-Q1")
                        .param("benchmark", "15.0"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getHighDenialRate_negative() throws Exception {
        when(service.getAdjustersWithHighDenialRate(15.0, "2026-Q1")).thenReturn(new java.util.ArrayList<>());

        mockMvc.perform(get("/api/adjusters/high-denial-rate")
                        .param("period", "2026-Q1")
                        .param("benchmark", "15.0"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/adjusters/sla-compliance-risk ───────────────────────────

    @Test
    void test_getSlaComplianceRisk_positive() throws Exception {
        List<AdjusterPerformanceDTO> list = Arrays.asList(dto);
        when(service.getAdjustersBelowSlaCompliance("2026-Q1")).thenReturn(list);

        mockMvc.perform(get("/api/adjusters/sla-compliance-risk")
                        .param("period", "2026-Q1"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getSlaComplianceRisk_notFound_returns404() throws Exception {
        when(service.getAdjustersBelowSlaCompliance("2026-Q1")).thenReturn(new java.util.ArrayList<>());

        mockMvc.perform(get("/api/adjusters/sla-compliance-risk")
                        .param("period", "2026-Q1"))
                .andExpect(status().isNoContent());
    }

    // ── DELETE /api/adjusters/performance/{perfId} ───────────────────────

    @Test
    void test_deletePerformance_positive() throws Exception {
        doNothing().when(service).deletePerformance(1L);

        mockMvc.perform(delete("/api/adjusters/performance/1"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Performance Resource Deleted"));
    }

    @Test
    void test_deletePerformance_notFound_returns404() throws Exception {
        doThrow(new RuntimeException()).when(service).deletePerformance(999L);

        mockMvc.perform(delete("/api/adjusters/performance/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Performance Resource Not Deleted"));
    }

    // ── PUT /api/adjusters/performance/{perfId} ──────────────────────────

    @Test
    void test_updatePerformance_positive() throws Exception {
        when(service.updatePerformance(eq(1L), any(AdjusterPerformanceDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/adjusters/performance/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfId").value(1))
                .andExpect(jsonPath("$.adjusterId").value(101));
    }

    @Test
    void test_updatePerformance_negative_notFound() throws Exception {
        when(service.updatePerformance(eq(999L), any(AdjusterPerformanceDTO.class))).thenThrow(new ResourceNotFoundException("Performance record not found for ID: 999"));

        mockMvc.perform(put("/api/adjusters/performance/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/adjusters/performance/{perfId} ────────────────────────

    @Test
    void test_partialUpdatePerformance_positive() throws Exception {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("errorRate", 5.5);

        when(service.patchPerformance(eq(1L), anyMap())).thenReturn(dto);

        mockMvc.perform(patch("/api/adjusters/performance/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());
    }

    @Test
    void test_partialUpdatePerformance_negative_invalidInput() throws Exception {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("errorRate", -10.0); // Invalid business logic

        when(service.patchPerformance(eq(1L), anyMap()))
                .thenThrow(new InvalidInputException("Numeric values out of valid range."));

        mockMvc.perform(patch("/api/adjusters/performance/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());
    }
}