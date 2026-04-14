package com.claim360.denialleakage.controller;

import com.claim360.denialleakage.dto.DenialPatternRequest;
import com.claim360.denialleakage.dto.DenialPatternResponse;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.service.DenialPatternService;
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

@WebMvcTest(DenialPatternController.class)
class DenialPatternControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DenialPatternService denialPatternService;

    private ObjectMapper objectMapper;
    private DenialPatternRequest request;
    private DenialPatternResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        request = new DenialPatternRequest();
        request.setClaimId("CLM-001");
        request.setDenialCode("CO-4");
        request.setReason("Service not covered");
        request.setOccurrenceDate(LocalDate.of(2024, 1, 15));

        response = new DenialPatternResponse(
                1L,
                "CLM-001",
                "CO-4",
                "Service not covered",
                LocalDate.of(2024, 1, 15)
        );
    }

    //POST
    @Test
    void createDenialPattern_ShouldReturn201() throws Exception {
        when(denialPatternService.createDenialPattern(any(DenialPatternRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/denial-patterns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patternId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"))
                .andExpect(jsonPath("$.denialCode").value("CO-4"))
                .andExpect(jsonPath("$.reason").value("Service not covered"));
    }

    @Test
    void createDenialPattern_WithMissingFields_ShouldReturn400() throws Exception {
        DenialPatternRequest invalidRequest = new DenialPatternRequest();

        mockMvc.perform(post("/api/denial-patterns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    //GET All
    @Test
    void getAllDenialPatterns_ShouldReturn200() throws Exception {
        when(denialPatternService.getAllDenialPatterns()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/denial-patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    //GET by ID
    @Test
    void getDenialPatternById_WhenExists_ShouldReturn200() throws Exception {
        when(denialPatternService.getDenialPatternById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/denial-patterns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patternId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    void getDenialPatternById_WhenNotExists_ShouldReturn404() throws Exception {
        when(denialPatternService.getDenialPatternById(99L))
                .thenThrow(new ResourceNotFoundException("DenialPattern", "patternId", 99L));

        mockMvc.perform(get("/api/denial-patterns/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    //GET by ClaimId
    @Test
    void getDenialPatternsByClaimId_WhenExists_ShouldReturn200() throws Exception {
        when(denialPatternService.getDenialPatternsByClaimId("CLM-001"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/denial-patterns/claim/CLM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    //GET by DenialCode
    @Test
    void getDenialPatternsByDenialCode_WhenExists_ShouldReturn200() throws Exception {
        when(denialPatternService.getDenialPatternsByDenialCode("CO-4"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/denial-patterns/code/CO-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].denialCode").value("CO-4"));
    }

    //GET by Keyword
    @Test
    void getDenialPatternsByReasonKeyword_WhenExists_ShouldReturn200() throws Exception {
        when(denialPatternService.getDenialPatternsByReasonKeyword("covered"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/denial-patterns/search/covered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Service not covered"));
    }

    //PUT
    @Test
    void updateDenialPattern_WhenExists_ShouldReturn200() throws Exception {
        when(denialPatternService.updateDenialPattern(eq(1L), any(DenialPatternRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/denial-patterns/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patternId").value(1L))
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    void updateDenialPattern_WhenNotExists_ShouldReturn404() throws Exception {
        when(denialPatternService.updateDenialPattern(eq(99L), any(DenialPatternRequest.class)))
                .thenThrow(new ResourceNotFoundException("DenialPattern", "patternId", 99L));

        mockMvc.perform(put("/api/denial-patterns/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    //DELETE
    @Test
    void deleteDenialPattern_WhenExists_ShouldReturn200() throws Exception {
        doNothing().when(denialPatternService).deleteDenialPattern(1L);

        mockMvc.perform(delete("/api/denial-patterns/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("DenialPattern with ID 1 deleted successfully."));
    }

    @Test
    void deleteDenialPattern_WhenNotExists_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("DenialPattern", "patternId", 99L))
                .when(denialPatternService).deleteDenialPattern(99L);

        mockMvc.perform(delete("/api/denial-patterns/99"))
                .andExpect(status().isNotFound());
    }
}