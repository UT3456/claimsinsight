
package com.claims.controller;

import com.claims.dto.request.ClaimReserveRequest;
import com.claims.dto.response.ClaimReserveResponse;
import com.claims.exception.ResourceNotFoundException;
import com.claims.service.ClaimReserveServiceImpl;
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
class ClaimReserveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimReserveServiceImpl claimReserveServiceImpl;

    private ObjectMapper objectMapper;
    private ClaimReserveRequest request;
    private ClaimReserveResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new ClaimReserveRequest("CLM001",
                new BigDecimal("15000.00"), LocalDate.of(2024, 1, 10));

        response = new ClaimReserveResponse(1L, "CLM001",
                new BigDecimal("15000.00"), LocalDate.of(2024, 1, 10));
    }

    // -------------------------------------------------------
    // POST
    // -------------------------------------------------------

    @Test
    void testCreateClaimReserve_returns201() throws Exception {
        when(claimReserveServiceImpl.createClaimReserve(any())).thenReturn(response);

        mockMvc.perform(post("/api/reserves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserveId").value(1))
                .andExpect(jsonPath("$.claimId").value("CLM001"))
                .andExpect(jsonPath("$.reserveAmount").value(15000.00));
    }

    @Test
    void testCreateClaimReserve_missingClaimId_returns400() throws Exception {
        ClaimReserveRequest invalid = new ClaimReserveRequest(null,
                new BigDecimal("15000.00"), LocalDate.of(2024, 1, 10));

        mockMvc.perform(post("/api/reserves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateClaimReserve_missingReserveAmount_returns400() throws Exception {
        ClaimReserveRequest invalid = new ClaimReserveRequest("CLM001",
                null, LocalDate.of(2024, 1, 10));

        mockMvc.perform(post("/api/reserves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateClaimReserve_missingUpdatedDate_returns400() throws Exception {
        ClaimReserveRequest invalid = new ClaimReserveRequest("CLM001",
                new BigDecimal("15000.00"), null);

        mockMvc.perform(post("/api/reserves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------
    // GET by ID
    // -------------------------------------------------------

    @Test
    void testGetClaimReserveById_returns200() throws Exception {
        when(claimReserveServiceImpl.getClaimReserveById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/reserves/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserveId").value(1))
                .andExpect(jsonPath("$.claimId").value("CLM001"))
                .andExpect(jsonPath("$.reserveAmount").value(15000.00));
    }

    @Test
    void testGetClaimReserveById_notFound_returns404() throws Exception {
        when(claimReserveServiceImpl.getClaimReserveById(99L))
                .thenThrow(new ResourceNotFoundException("ClaimReserve", "reserveId", 99L));

        mockMvc.perform(get("/api/reserves/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // GET All
    // -------------------------------------------------------

    @Test
    void testGetAllClaimReserves_returns200() throws Exception {
        when(claimReserveServiceImpl.getAllClaimReserves()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reserves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetAllClaimReserves_emptyList_returns200() throws Exception {
        when(claimReserveServiceImpl.getAllClaimReserves()).thenReturn(List.of());

        mockMvc.perform(get("/api/reserves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------
    // PUT
    // -------------------------------------------------------

    @Test
    void testUpdateClaimReserve_returns200() throws Exception {
        when(claimReserveServiceImpl.updateClaimReserve(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/reserves/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserveId").value(1));
    }

    @Test
    void testUpdateClaimReserve_notFound_returns404() throws Exception {
        when(claimReserveServiceImpl.updateClaimReserve(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("ClaimReserve", "reserveId", 99L));

        mockMvc.perform(put("/api/reserves/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // DELETE
    // -------------------------------------------------------

    @Test
    void testDeleteClaimReserve_returns200() throws Exception {
        doNothing().when(claimReserveServiceImpl).deleteClaimReserve(1L);

        mockMvc.perform(delete("/api/reserves/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ClaimReserve deleted successfully"));
    }

    @Test
    void testDeleteClaimReserve_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("ClaimReserve", "reserveId", 99L))
                .when(claimReserveServiceImpl).deleteClaimReserve(99L);

        mockMvc.perform(delete("/api/reserves/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // Query Endpoints
    // -------------------------------------------------------

    @Test
    void testGetReservesByClaimId_returns200() throws Exception {
        when(claimReserveServiceImpl.getReservesByClaimId("CLM001")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reserves/claim/CLM001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM001"));
    }

    @Test
    void testGetReservesByClaimId_notFound_returns404() throws Exception {
        when(claimReserveServiceImpl.getReservesByClaimId("CLM999"))
                .thenThrow(new ResourceNotFoundException("No reserve records found for claimId: CLM999"));

        mockMvc.perform(get("/api/reserves/claim/CLM999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetReservesByDateRange_returns200() throws Exception {
        when(claimReserveServiceImpl.getReservesByDateRange(any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/reserves/date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // -------------------------------------------------------
    // Analytics Endpoints
    // -------------------------------------------------------

    @Test
    void testGetLatestReserveForClaim_returns200() throws Exception {
        when(claimReserveServiceImpl.getLatestReserveForClaim("CLM001")).thenReturn(response);

        mockMvc.perform(get("/api/reserves/claim/CLM001/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value("CLM001"))
                .andExpect(jsonPath("$.reserveAmount").value(15000.00));
    }

    @Test
    void testGetLatestReserveForClaim_notFound_returns404() throws Exception {
        when(claimReserveServiceImpl.getLatestReserveForClaim("CLM999"))
                .thenThrow(new ResourceNotFoundException("No reserve records found for claimId: CLM999"));

        mockMvc.perform(get("/api/reserves/claim/CLM999/latest"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTotalReserveAmount_returns200() throws Exception {
        when(claimReserveServiceImpl.getTotalReserveAmount())
                .thenReturn(new BigDecimal("50000.00"));

        mockMvc.perform(get("/api/reserves/analytics/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReserveAmount").value(50000.00));
    }

    @Test
    void testGetReserveHistory_returns200() throws Exception {
        ClaimReserveResponse r2 = new ClaimReserveResponse(2L, "CLM001",
                new BigDecimal("18000.00"), LocalDate.of(2024, 3, 1));

        when(claimReserveServiceImpl.getReserveHistoryByClaimId("CLM001"))
                .thenReturn(List.of(response, r2));

        mockMvc.perform(get("/api/reserves/claim/CLM001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetLatestReserveSummaryAllClaims_returns200() throws Exception {
        when(claimReserveServiceImpl.getLatestReserveSummaryAllClaims())
                .thenReturn(Map.of("CLM001", new BigDecimal("15000.00"),
                        "CLM002", new BigDecimal("8000.00")));

        mockMvc.perform(get("/api/reserves/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CLM001").value(15000.00));
    }

    @Test
    void testGetMonthlyReserveTrend_returns200() throws Exception {
        when(claimReserveServiceImpl.getMonthlyReserveTrendByClaimId("CLM001"))
                .thenReturn(Map.of("2024-01", new BigDecimal("10000.00"),
                        "2024-03", new BigDecimal("15000.00")));

        mockMvc.perform(get("/api/reserves/claim/CLM001/trend/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['2024-01']").value(10000.00));
    }

    @Test
    void testGetReserveAdequacy_adequate_returns200() throws Exception {
        when(claimReserveServiceImpl.getReserveAdequacyForClaim("CLM001", new BigDecimal("12000.00")))
                .thenReturn(Map.of(
                        "claimId", "CLM001",
                        "latestReserve", new BigDecimal("15000.00"),
                        "totalCost", new BigDecimal("12000.00"),
                        "variance", new BigDecimal("3000.00"),
                        "adequacyStatus", "ADEQUATE"
                ));

        mockMvc.perform(get("/api/reserves/claim/CLM001/adequacy")
                        .param("totalCost", "12000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adequacyStatus").value("ADEQUATE"))
                .andExpect(jsonPath("$.claimId").value("CLM001"));
    }

    @Test
    void testGetReserveAdequacy_underReserved_returns200() throws Exception {
        when(claimReserveServiceImpl.getReserveAdequacyForClaim("CLM001", new BigDecimal("20000.00")))
                .thenReturn(Map.of(
                        "claimId", "CLM001",
                        "latestReserve", new BigDecimal("15000.00"),
                        "totalCost", new BigDecimal("20000.00"),
                        "variance", new BigDecimal("-5000.00"),
                        "adequacyStatus", "UNDER_RESERVED"
                ));

        mockMvc.perform(get("/api/reserves/claim/CLM001/adequacy")
                        .param("totalCost", "20000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adequacyStatus").value("UNDER_RESERVED"));
    }

    @Test
    void testGetReserveAdequacy_notFound_returns404() throws Exception {
        when(claimReserveServiceImpl.getReserveAdequacyForClaim("CLM999", new BigDecimal("5000.00")))
                .thenThrow(new ResourceNotFoundException("No reserve records found for claimId: CLM999"));

        mockMvc.perform(get("/api/reserves/claim/CLM999/adequacy")
                        .param("totalCost", "5000.00"))
                .andExpect(status().isNotFound());
    }
}