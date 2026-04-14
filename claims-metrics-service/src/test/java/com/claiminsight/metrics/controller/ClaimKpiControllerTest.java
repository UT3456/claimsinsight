package com.claiminsight.metrics.controller;

import com.claiminsight.metrics.dto.ClaimKpiRequestDTO;
import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.dto.KpiSummaryDTO;
import com.claiminsight.metrics.exception.GlobalExceptionHandler;
import com.claiminsight.metrics.exception.ResourceNotFoundException;
import com.claiminsight.metrics.service.ClaimKpiService;
import com.claiminsight.metrics.service.KpiCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClaimKpiController.class)
@Import(GlobalExceptionHandler.class)
class ClaimKpiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ClaimKpiService claimKpiService;
    @MockitoBean KpiCalculationService kpiCalculationService;

    private ObjectMapper objectMapper;
    private ClaimKpiResponseDTO response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        response = new ClaimKpiResponseDTO();
        response.setKpiId(1L);
        response.setClaimId("CLM-001");
        response.setMetricName("TAT");
        response.setMetricValue(new BigDecimal("5.0"));
        response.setMetricDate(LocalDate.of(2026, 1, 15));
    }

    @Test
    void createKpi_returns201() throws Exception {
        ClaimKpiRequestDTO req = new ClaimKpiRequestDTO();
        req.setClaimId("CLM-001");
        req.setMetricName("TAT");
        req.setMetricValue(new BigDecimal("5.0"));
        req.setMetricDate(LocalDate.of(2026, 1, 15));

        when(claimKpiService.createKpi(any())).thenReturn(response);

        mockMvc.perform(post("/api/kpis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kpiId").value(1));
    }

    @Test
    void createKpi_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/kpis")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllKpis_returns200() throws Exception {
        when(claimKpiService.getAllKpis()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getKpiById_returns200() throws Exception {
        when(claimKpiService.getKpiById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/kpis/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpiId").value(1));
    }

    @Test
    void getKpiById_notFound_returns404() throws Exception {
        when(claimKpiService.getKpiById(99L))
                .thenThrow(new ResourceNotFoundException("KPI not found"));

        mockMvc.perform(get("/api/kpis/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getKpisByClaimId_returns200() throws Exception {
        when(claimKpiService.getKpisByClaimId("CLM-001")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/kpis/claim/CLM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    @Test
    void getKpisByMetricName_returns200() throws Exception {
        when(claimKpiService.getKpisByMetricName("TAT")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/kpis/metric/TAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getKpisByDateRange_returns200() throws Exception {
        when(claimKpiService.getKpisByDateRange(any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/kpis/date-range")
                .param("start", "2026-01-01")
                .param("end", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteKpi_returns204() throws Exception {
        doNothing().when(claimKpiService).deleteKpi(1L);

        mockMvc.perform(delete("/api/kpis/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteKpi_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("not found"))
                .when(claimKpiService).deleteKpi(99L);

        mockMvc.perform(delete("/api/kpis/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void calculateKpis_returns201() throws Exception {
        KpiSummaryDTO summary = new KpiSummaryDTO(
                "CLM-001",
                new BigDecimal("45.0"),
                new BigDecimal("3.0"),
                new BigDecimal("5.5"),
                new BigDecimal("2.0"),
                new BigDecimal("0.85"),
                new BigDecimal("49.0"),
                LocalDate.now(),
                List.of()
        );
        when(kpiCalculationService.calculateAndSave("CLM-001")).thenReturn(summary);

        mockMvc.perform(post("/api/kpis/calculate/CLM-001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.tat").value(45.0));
    }
}
