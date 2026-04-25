package com.demo.controllertesting;

import com.demo.controller.SLAViolationController;
import com.demo.dto.SLAViolationDTO;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.exception.ResourceNotFoundException;
import com.demo.service.SLAViolationService;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SLAViolationController.
 */
@ExtendWith(MockitoExtension.class)
public class SLAViolationControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SLAViolationService service;

    @InjectMocks
    private SLAViolationController controller;

    private SLAViolationDTO dto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

        dto = new SLAViolationDTO();
        dto.setViolationId(1L);
        dto.setClaimId(10L);
        dto.setAdjusterId(101L);
        dto.setViolationType("FINAL_SETTLEMENT");
        dto.setSlaTargetDays(10);
        dto.setActualDays(15);
        dto.setViolationDate(new Date());

        dto.setDaysOverdue(5);
        dto.setSeverity("MEDIUM");
    }

    // ── POST /api/sla-violations ─────────────────────────────────────────

    @Test
    void test_recordViolation_positive() throws Exception {
        SLAViolationDTO inputDto = new SLAViolationDTO();
        inputDto.setClaimId(10L);
        inputDto.setAdjusterId(101L);
        inputDto.setViolationType("FINAL_SETTLEMENT");
        inputDto.setSlaTargetDays(10);
        inputDto.setActualDays(15);
        inputDto.setViolationDate(new Date());
        // Do NOT set daysOverdue, severity, escalated — these are computed output fields

        when(service.recordSLAViolation(any(SLAViolationDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/sla-violations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().string("SLA Violation Recorded Successfully"));
    }

    @Test
    void test_recordViolation_negative() throws Exception {
        when(service.recordSLAViolation(any(SLAViolationDTO.class))).thenReturn(null);

        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post("/api/sla-violations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("SLA Violation Recording Failed"));
    }

    // ── GET /api/sla-violations/adjuster/{id} ────────────────────────────

    @Test
    void test_getByAdjuster_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getSLAViolationsByAdjuster(101L)).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/adjuster/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adjusterId").value(101));
    }

    @Test
    void test_getByAdjuster_negative() throws Exception {
        when(service.getSLAViolationsByAdjuster(999L)).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/adjuster/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/sla-violations/adjuster/{id}/escalations ────────────────

    @Test
    void test_getEscalations_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getEscalationCandidatesByAdjuster(101L)).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/adjuster/101/escalations"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getEscalations_negative() throws Exception {
        when(service.getEscalationCandidatesByAdjuster(101L)).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/adjuster/101/escalations"))
                .andExpect(status().isNoContent());
    }
    //-Get by AdjusterAndDateRange

    @Test
    void test_getByAdjusterAndDateRange_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        // Mocking the service to return a list when called with specific ID and any dates
        when(service.getSLAViolationsByAdjusterAndDateRange(eq(101L), any(Date.class), any(Date.class)))
                .thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/adjuster/101/date-range")
                        .param("start", "2026-01-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].adjusterId").value(101));
    }

    @Test
    void test_getByAdjusterAndDateRange_negative() throws Exception {
        // Mocking the service to return an empty list
        when(service.getSLAViolationsByAdjusterAndDateRange(eq(101L), any(Date.class), any(Date.class)))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/sla-violations/adjuster/101/date-range")
                        .param("start", "2026-01-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isNoContent());
    }



    // ── GET /api/sla-violations/adjuster/{id}/count ───────────────────────

    @Test
    void test_countByAdjusterAndPeriod_positive() throws Exception {
        when(service.countViolationsByAdjusterAndPeriod(eq(101L), any(Date.class), any(Date.class))).thenReturn(3L);

        mockMvc.perform(get("/api/sla-violations/adjuster/101/count")
                        .param("start", "2026-01-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void test_countByAdjusterAndPeriod_negative() throws Exception {
        when(service.countViolationsByAdjusterAndPeriod(eq(999L), any(Date.class), any(Date.class))).thenReturn(null);

        mockMvc.perform(get("/api/sla-violations/adjuster/999/count")
                        .param("start", "2026-01-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/sla-violations/claim/{claimId} ───────────────────────────

    @Test
    void test_getByClaim_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getSLAViolationsByClaim(10L)).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/claim/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value(10));
    }

    @Test
    void test_getByClaim_negative() throws Exception {
        when(service.getSLAViolationsByClaim(999L)).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/claim/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/sla-violations/claim/{claimId}/total-overdue ─────────────

    @Test
    void test_getTotalOverdue_positive() throws Exception {
        when(service.getTotalDaysOverdueByClaim(10L)).thenReturn(15);

        mockMvc.perform(get("/api/sla-violations/claim/10/total-overdue"))
                .andExpect(status().isOk())
                .andExpect(content().string("15"));
    }

    @Test
    void test_getTotalOverdue_negative() throws Exception {
        when(service.getTotalDaysOverdueByClaim(99L)).thenReturn(null);

        mockMvc.perform(get("/api/sla-violations/claim/99/total-overdue"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/sla-violations/type ──────────────────────────────────────

    @Test
    void test_getByType_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getViolationsByType("PAYMENT_DELAY")).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/type")
                        .param("violationType", "PAYMENT_DELAY"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getByType_negative() throws Exception {
        when(service.getViolationsByType("UNKNOWN")).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/type")
                        .param("violationType", "UNKNOWN"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/sla-violations/severity ─────────────────────────────────

    @Test
    void test_getBySeverity_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getViolationsBySeverity("HIGH")).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/severity")
                        .param("level", "HIGH"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getBySeverity_negative() throws Exception {
        when(service.getViolationsBySeverity("LOW")).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/severity")
                        .param("level", "LOW"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/sla-violations/overdue ───────────────────────────────────

    @Test
    void test_getByDaysOverdue_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getViolationsByDaysOverdueGreaterThan(5)).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/overdue")
                        .param("days", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void test_getByDaysOverdue_negative() throws Exception {
        when(service.getViolationsByDaysOverdueGreaterThan(100)).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/overdue")
                        .param("days", "100"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/sla-violations/date-range ───────────────────────────────

    @Test
    void test_getByDateRange_positive() throws Exception {
        List<SLAViolationDTO> list = Arrays.asList(dto);
        when(service.getViolationsByDateRange(any(Date.class), any(Date.class))).thenReturn(list);

        mockMvc.perform(get("/api/sla-violations/date-range")
                        .param("start", "2026-01-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void test_getByDateRange_negative() throws Exception {
        when(service.getViolationsByDateRange(any(Date.class), any(Date.class))).thenReturn(new ArrayList<SLAViolationDTO>());

        mockMvc.perform(get("/api/sla-violations/date-range")
                        .param("start", "2026-01-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isNoContent());
    }

    // ── DELETE /api/sla-violations/{violationId} ──────────────────────────

    @Test
    void test_deleteViolation_positive() throws Exception {
        doNothing().when(service).deleteViolation(1L);

        mockMvc.perform(delete("/api/sla-violations/1"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("SLA Violation Deleted Successfully"));
    }

    @Test
    void test_deleteViolation_negative() throws Exception {
        doThrow(new RuntimeException()).when(service).deleteViolation(999L);

        mockMvc.perform(delete("/api/sla-violations/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("SLA Violation Deletion Failed"));
    }

    // ── PUT /api/sla-violations/{violationId} ─────────────────────────────

    @Test
    void test_updateViolation_positive() throws Exception {
        when(service.updateSLAViolation(eq(1L), any(SLAViolationDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/sla-violations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print()) // This will print the error list if it fails again
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violationId").value(1));
    }

    // ── PATCH /api/sla-violations/{violationId}/escalate ──────────────────

    @Test
    void test_toggleEscalation_positive() throws Exception {
        dto.setEscalated(true);
        when(service.updateEscalationStatus(1L, true)).thenReturn(dto);

        mockMvc.perform(patch("/api/sla-violations/1/escalate")
                        .param("escalated", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(true));

        verify(service).updateEscalationStatus(1L, true);
    }

    // ── PATCH /api/sla-violations/{violationId}/severity ──────────────────

    @Test
    void test_updateSeverity_positive() throws Exception {
        dto.setSeverity("CRITICAL");
        when(service.changeViolationSeverity(1L, "CRITICAL")).thenReturn(dto);

        mockMvc.perform(patch("/api/sla-violations/1/severity")
                        .param("newLevel", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("CRITICAL"));

        verify(service).changeViolationSeverity(1L, "CRITICAL");
    }

    // ── Negative Cases for Update/Patch ───────────────────────────────────

    @Test
    void test_updateViolation_notFound() throws Exception {
        when(service.updateSLAViolation(eq(999L), any(SLAViolationDTO.class)))
                .thenThrow(new ResourceNotFoundException("SLA Violation not found: 999"));

        mockMvc.perform(put("/api/sla-violations/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
