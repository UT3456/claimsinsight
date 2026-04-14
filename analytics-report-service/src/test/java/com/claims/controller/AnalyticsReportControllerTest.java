package com.claims.controller;

import com.claims.dto.request.AnalyticsReportRequest;
import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.enums.ReportScope;
import com.claims.exception.ResourceNotFoundException;
import com.claims.service.AnalyticsReportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsReportServiceImpl analyticsReportService;

    private ObjectMapper objectMapper;
    private AnalyticsReportRequest request;
    private AnalyticsReportResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new AnalyticsReportRequest(ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 1, 10), "admin", "{\"tat\":5.2}");

        response = new AnalyticsReportResponse(1L, ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 1, 10), "admin", "{\"tat\":5.2}");
    }

    // -------------------------------------------------------
    // POST
    // -------------------------------------------------------

    @Test
    void testCreateReport_returns201() throws Exception {
        when(analyticsReportService.createReport(any())).thenReturn(response);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.scope").value("REGION"))
                .andExpect(jsonPath("$.scopeValue").value("North Region"))
                .andExpect(jsonPath("$.generatedBy").value("admin"));
    }

    @Test
    void testCreateReport_missingScope_returns400() throws Exception {
        AnalyticsReportRequest invalid = new AnalyticsReportRequest(
                null, "North Region", "TAT",
                LocalDate.of(2024, 1, 10), "admin", "{}");

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateReport_missingGeneratedBy_returns400() throws Exception {
        AnalyticsReportRequest invalid = new AnalyticsReportRequest(
                ReportScope.REGION, "North Region", "TAT",
                LocalDate.of(2024, 1, 10), null, "{}");

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------
    // GET by ID
    // -------------------------------------------------------

    @Test
    void testGetReportById_returns200() throws Exception {
        when(analyticsReportService.getReportById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.scope").value("REGION"))
                .andExpect(jsonPath("$.scopeValue").value("North Region"));
    }

    @Test
    void testGetReportById_notFound_returns404() throws Exception {
        when(analyticsReportService.getReportById(99L))
                .thenThrow(new ResourceNotFoundException("AnalyticsReport", "reportId", 99L));

        mockMvc.perform(get("/api/reports/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // GET All
    // -------------------------------------------------------

    @Test
    void testGetAllReports_returns200() throws Exception {
        when(analyticsReportService.getAllReports()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetAllReports_emptyList_returns200() throws Exception {
        when(analyticsReportService.getAllReports()).thenReturn(List.of());

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------
    // PUT
    // -------------------------------------------------------

    @Test
    void testUpdateReport_returns200() throws Exception {
        when(analyticsReportService.updateReport(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/reports/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1));
    }

    @Test
    void testUpdateReport_notFound_returns404() throws Exception {
        when(analyticsReportService.updateReport(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("AnalyticsReport", "reportId", 99L));

        mockMvc.perform(put("/api/reports/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------

    @Test
    void testDeleteReport_returns200() throws Exception {
        doNothing().when(analyticsReportService).deleteReport(1L);

        mockMvc.perform(delete("/api/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("AnalyticsReport deleted successfully"));
    }

    @Test
    void testDeleteReport_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("AnalyticsReport", "reportId", 99L))
                .when(analyticsReportService).deleteReport(99L);

        mockMvc.perform(delete("/api/reports/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // Query Endpoints
    // -------------------------------------------------------

    @Test
    void testGetReportsByScope_returns200() throws Exception {
        when(analyticsReportService.getReportsByScope(ReportScope.REGION))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports/scope/REGION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].scope").value("REGION"));
    }

    @Test
    void testGetReportsByScope_notFound_returns404() throws Exception {
        when(analyticsReportService.getReportsByScope(ReportScope.CLAIM_TYPE))
                .thenThrow(new ResourceNotFoundException("No reports found for scope: CLAIM_TYPE"));

        mockMvc.perform(get("/api/reports/scope/CLAIM_TYPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetReportsByScopeValue_returns200() throws Exception {
        when(analyticsReportService.getReportsByScopeValue("NorthRegion"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports/scope-value/NorthRegion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetReportsByScopeAndScopeValue_returns200() throws Exception {
        when(analyticsReportService.getReportsByScopeAndScopeValue(ReportScope.REGION, "NorthRegion"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports/scope/REGION/scope-value/NorthRegion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetReportsByDateRange_returns200() throws Exception {
        when(analyticsReportService.getReportsByDateRange(any(), any()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetReportsByGeneratedBy_returns200() throws Exception {
        when(analyticsReportService.getReportsByGeneratedBy("admin"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/reports/generated-by/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // -------------------------------------------------------
    // Analytics Endpoints
    // -------------------------------------------------------

    @Test
    void testGetLatestReport_returns200() throws Exception {
        when(analyticsReportService.getLatestReport(ReportScope.REGION, "NorthRegion"))
                .thenReturn(response);

        mockMvc.perform(get("/api/reports/latest")
                        .param("scope", "REGION")
                        .param("scopeValue", "NorthRegion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeValue").value("North Region"));
    }

    @Test
    void testGetLatestReport_notFound_returns404() throws Exception {
        when(analyticsReportService.getLatestReport(ReportScope.REGION, "Unknown"))
                .thenThrow(new ResourceNotFoundException("No report found"));

        mockMvc.perform(get("/api/reports/latest")
                        .param("scope", "REGION")
                        .param("scopeValue", "Unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetReportCountByScope_returns200() throws Exception {
        when(analyticsReportService.getReportCountByScope())
                .thenReturn(Map.of("REGION", 3L, "PRODUCT", 2L));

        mockMvc.perform(get("/api/reports/analytics/count-by-scope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.REGION").value(3))
                .andExpect(jsonPath("$.PRODUCT").value(2));
    }

    @Test
    void testGetReportCountByScopeValue_returns200() throws Exception {
        when(analyticsReportService.getReportCountByScopeValue())
                .thenReturn(Map.of("NorthRegion", 5L, "Auto", 3L));

        mockMvc.perform(get("/api/reports/analytics/count-by-scope-value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NorthRegion").value(5));
    }

    @Test
    void testGetMonthlyReportGenerationTrend_returns200() throws Exception {
        when(analyticsReportService.getMonthlyReportGenerationTrend())
                .thenReturn(Map.of("2024-01", 5L, "2024-02", 8L));

        mockMvc.perform(get("/api/reports/analytics/trend/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['2024-01']").value(5));
    }

    @Test
    void testGetMostActiveReportGenerator_returns200() throws Exception {
        when(analyticsReportService.getMostActiveReportGenerator())
                .thenReturn(Map.of("generatedBy", "admin", "reportCount", 10L));

        mockMvc.perform(get("/api/reports/analytics/most-active-generator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedBy").value("admin"))
                .andExpect(jsonPath("$.reportCount").value(10));
    }

    @Test
    void testGetReportDashboardSummary_returns200() throws Exception {
        when(analyticsReportService.getReportDashboardSummary())
                .thenReturn(Map.of(
                        "totalReports", 10L,
                        "mostActiveGenerator", "admin"
                ));

        mockMvc.perform(get("/api/reports/analytics/dashboard-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").value(10))
                .andExpect(jsonPath("$.mostActiveGenerator").value("admin"));
    }
}