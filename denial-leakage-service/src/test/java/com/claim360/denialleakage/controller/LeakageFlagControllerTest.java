package com.claim360.denialleakage.controller;

import com.claim360.denialleakage.dto.LeakageFlagRequest;
import com.claim360.denialleakage.dto.LeakageFlagResponse;
import com.claim360.denialleakage.enums.LeakageType;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.service.LeakageFlagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeakageFlagController.class)
class LeakageFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeakageFlagService leakageFlagService;

    private ObjectMapper objectMapper;
    private LeakageFlagRequest request;
    private LeakageFlagResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new LeakageFlagRequest();
        request.setClaimId("CLM-001");
        request.setLeakageType(LeakageType.Overpayment);
        request.setEstimatedLoss(5000.0);
        request.setIdentifiedDate(LocalDate.of(2024, 1, 15));

        response = new LeakageFlagResponse(
                1L,
                "CLM-001",
                LeakageType.Overpayment,
                5000.0,
                LocalDate.of(2024, 1, 15)
        );
    }

    //POST
    @Test
    void createLeakageFlag_ShouldReturn201() throws Exception {
        when(leakageFlagService.createLeakageFlag(any(LeakageFlagRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/leakage-flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leakageId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.leakageType").value("Overpayment"))
                .andExpect(jsonPath("$.estimatedLoss").value(5000.0));
    }

    @Test
    void createLeakageFlag_WithMissingFields_ShouldReturn400() throws Exception {
        LeakageFlagRequest invalidRequest = new LeakageFlagRequest();

        mockMvc.perform(post("/api/leakage-flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    //GET All
    @Test
    void getAllLeakageFlags_ShouldReturn200() throws Exception {
        when(leakageFlagService.getAllLeakageFlags()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/leakage-flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    //GET by ID
    @Test
    void getLeakageFlagById_WhenExists_ShouldReturn200() throws Exception {
        when(leakageFlagService.getLeakageFlagById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/leakage-flags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leakageId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    void getLeakageFlagById_WhenNotExists_ShouldReturn404() throws Exception {
        when(leakageFlagService.getLeakageFlagById(99L))
                .thenThrow(new ResourceNotFoundException("LeakageFlag", "leakageId", 99L));

        mockMvc.perform(get("/api/leakage-flags/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // GETby ClaimId
    @Test
    void getLeakageFlagsByClaimId_WhenExists_ShouldReturn200() throws Exception {
        when(leakageFlagService.getLeakageFlagsByClaimId("CLM-001"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/leakage-flags/claim/CLM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    //GET by LeakageType
    @Test
    void getLeakageFlagsByLeakageType_WhenExists_ShouldReturn200() throws Exception {
        when(leakageFlagService.getLeakageFlagsByLeakageType(LeakageType.Overpayment))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/leakage-flags/type/Overpayment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leakageType").value("Overpayment"));
    }

    //GET by EstimatedLoss
    @Test
    void getLeakageFlagsByEstimatedLoss_WhenExists_ShouldReturn200() throws Exception {
        when(leakageFlagService.getLeakageFlagsByEstimatedLoss(1000.0))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/leakage-flags/loss/1000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estimatedLoss").value(5000.0));
    }

    //PUT
    @Test
    void updateLeakageFlag_WhenExists_ShouldReturn200() throws Exception {
        when(leakageFlagService.updateLeakageFlag(eq(1L), any(LeakageFlagRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/leakage-flags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leakageId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    void updateLeakageFlag_WhenNotExists_ShouldReturn404() throws Exception {
        when(leakageFlagService.updateLeakageFlag(eq(99L), any(LeakageFlagRequest.class)))
                .thenThrow(new ResourceNotFoundException("LeakageFlag", "leakageId", 99L));

        mockMvc.perform(put("/api/leakage-flags/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    //DELETE
    @Test
    void deleteLeakageFlag_WhenExists_ShouldReturn200() throws Exception {
        doNothing().when(leakageFlagService).deleteLeakageFlag(1L);

        mockMvc.perform(delete("/api/leakage-flags/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("LeakageFlag with ID 1 deleted successfully."));
    }

    @Test
    void deleteLeakageFlag_WhenNotExists_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("LeakageFlag", "leakageId", 99L))
                .when(leakageFlagService).deleteLeakageFlag(99L);

        mockMvc.perform(delete("/api/leakage-flags/99"))
                .andExpect(status().isNotFound());
    }
}