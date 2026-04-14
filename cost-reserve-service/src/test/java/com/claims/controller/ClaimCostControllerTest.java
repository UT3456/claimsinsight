package com.claims.controller;

import com.claims.dto.request.ClaimCostRequest;
import com.claims.dto.response.ClaimCostResponse;
import com.claims.enums.CostType;
import com.claims.exception.ResourceNotFoundException;
import com.claims.service.ClaimCostServiceImpl;
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

import java.math.BigDecimal;
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
class ClaimCostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimCostServiceImpl claimCostServiceImpl;

    private ObjectMapper objectMapper;
    private ClaimCostRequest request;
    private ClaimCostResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new ClaimCostRequest("CLM001", CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));

        response = new ClaimCostResponse(1L, "CLM001", CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));
    }

    // -------------------------------------------------------
    // POST
    // -------------------------------------------------------

    @Test
    void testCreateClaimCost_returns201() throws Exception {
        when(claimCostServiceImpl.createClaimCost(any())).thenReturn(response);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costId").value(1))
                .andExpect(jsonPath("$.claimId").value("CLM001"))
                .andExpect(jsonPath("$.costType").value("MEDICAL"));
    }

    @Test
    void testCreateClaimCost_missingClaimId_returns400() throws Exception {
        ClaimCostRequest invalid = new ClaimCostRequest(null, CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateClaimCost_missingCostType_returns400() throws Exception {
        ClaimCostRequest invalid = new ClaimCostRequest("CLM001", null,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateClaimCost_missingAmount_returns400() throws Exception {
        ClaimCostRequest invalid = new ClaimCostRequest("CLM001", CostType.MEDICAL,
                null, LocalDate.of(2024, 1, 15));

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------
    // GET by ID
    // -------------------------------------------------------

    @Test
    void testGetClaimCostById_returns200() throws Exception {
        when(claimCostServiceImpl.getClaimCostById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/costs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costId").value(1))
                .andExpect(jsonPath("$.claimId").value("CLM001"))
                .andExpect(jsonPath("$.costType").value("MEDICAL"));
    }

    @Test
    void testGetClaimCostById_notFound_returns404() throws Exception {
        when(claimCostServiceImpl.getClaimCostById(99L))
                .thenThrow(new ResourceNotFoundException("ClaimCost", "costId", 99L));

        mockMvc.perform(get("/api/costs/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // GET All
    // -------------------------------------------------------

    @Test
    void testGetAllClaimCosts_returns200() throws Exception {
        when(claimCostServiceImpl.getAllClaimCosts()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/costs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetAllClaimCosts_emptyList_returns200() throws Exception {
        when(claimCostServiceImpl.getAllClaimCosts()).thenReturn(List.of());

        mockMvc.perform(get("/api/costs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------
    // PUT
    // -------------------------------------------------------

    @Test
    void testUpdateClaimCost_returns200() throws Exception {
        when(claimCostServiceImpl.updateClaimCost(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/costs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costId").value(1));
    }

    @Test
    void testUpdateClaimCost_notFound_returns404() throws Exception {
        when(claimCostServiceImpl.updateClaimCost(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("ClaimCost", "costId", 99L));

        mockMvc.perform(put("/api/costs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------

    @Test
    void testDeleteClaimCost_returns200() throws Exception {
        doNothing().when(claimCostServiceImpl).deleteClaimCost(1L);

        mockMvc.perform(delete("/api/costs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ClaimCost deleted successfully"));
    }

    @Test
    void testDeleteClaimCost_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("ClaimCost", "costId", 99L))
                .when(claimCostServiceImpl).deleteClaimCost(99L);

        mockMvc.perform(delete("/api/costs/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // Query Endpoints
    // -------------------------------------------------------

    @Test
    void testGetCostsByClaimId_returns200() throws Exception {
        when(claimCostServiceImpl.getCostsByClaimId("CLM001")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/costs/claim/CLM001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM001"));
    }

    @Test
    void testGetCostsByClaimId_notFound_returns404() throws Exception {
        when(claimCostServiceImpl.getCostsByClaimId("CLM999"))
                .thenThrow(new ResourceNotFoundException("No cost records found for claimId: CLM999"));

        mockMvc.perform(get("/api/costs/claim/CLM999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCostsByCostType_returns200() throws Exception {
        when(claimCostServiceImpl.getCostsByCostType(CostType.MEDICAL)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/costs/type/MEDICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].costType").value("MEDICAL"));
    }

    @Test
    void testGetCostsByClaimIdAndType_returns200() throws Exception {
        when(claimCostServiceImpl.getCostsByClaimIdAndType("CLM001", CostType.MEDICAL))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/costs/claim/CLM001/type/MEDICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetCostsByDateRange_returns200() throws Exception {
        when(claimCostServiceImpl.getCostsByDateRange(any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/costs/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // -------------------------------------------------------
    // Analytics Endpoints
    // -------------------------------------------------------

    @Test
    void testGetTotalCostByClaimId_returns200() throws Exception {
        when(claimCostServiceImpl.getTotalCostByClaimId("CLM001"))
                .thenReturn(new BigDecimal("7000.00"));

        mockMvc.perform(get("/api/costs/claim/CLM001/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value("CLM001"))
                .andExpect(jsonPath("$.totalCost").value(7000.00));
    }

    @Test
    void testGetTotalCostByClaimId_notFound_returns404() throws Exception {
        when(claimCostServiceImpl.getTotalCostByClaimId("CLM999"))
                .thenThrow(new ResourceNotFoundException("No cost records found for claimId: CLM999"));

        mockMvc.perform(get("/api/costs/claim/CLM999/total"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCostBreakdownByType_returns200() throws Exception {
        when(claimCostServiceImpl.getCostBreakdownByTypeForClaim("CLM001"))
                .thenReturn(Map.of("MEDICAL", new BigDecimal("5000.00"),
                        "LEGAL", new BigDecimal("2000.00")));

        mockMvc.perform(get("/api/costs/claim/CLM001/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MEDICAL").value(5000.00));
    }

    @Test
    void testGetOverallCostSummaryByType_returns200() throws Exception {
        when(claimCostServiceImpl.getOverallCostSummaryByType())
                .thenReturn(Map.of("MEDICAL", new BigDecimal("15000.00"),
                        "LEGAL", new BigDecimal("8000.00")));

        mockMvc.perform(get("/api/costs/summary/by-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MEDICAL").value(15000.00));
    }

    @Test
    void testGetMonthlyCostTrend_returns200() throws Exception {
        when(claimCostServiceImpl.getMonthlyCostTrendByClaimId("CLM001"))
                .thenReturn(Map.of("2024-01", new BigDecimal("3000.00"),
                        "2024-02", new BigDecimal("4500.00")));

        mockMvc.perform(get("/api/costs/claim/CLM001/trend/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['2024-01']").value(3000.00));
    }

    @Test
    void testGetHighestCostClaim_returns200() throws Exception {
        when(claimCostServiceImpl.getHighestCostClaim())
                .thenReturn(Map.of("claimId", "CLM001", "totalCost", new BigDecimal("12000.00")));

        mockMvc.perform(get("/api/costs/analytics/highest-cost-claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value("CLM001"));
    }
}