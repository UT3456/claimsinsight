package com.claiminsight.ingestion.controller;

import com.claiminsight.ingestion.dto.IngestionRequestDTO;
import com.claiminsight.ingestion.dto.IngestionResponseDTO;
import com.claiminsight.ingestion.exception.GlobalExceptionHandler;
import com.claiminsight.ingestion.exception.ResourceNotFoundException;
import com.claiminsight.ingestion.service.IngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IngestionController.class)
@Import(GlobalExceptionHandler.class)
class IngestionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IngestionService ingestionService;
    @Autowired ObjectMapper objectMapper;

    private IngestionResponseDTO response;

    @BeforeEach
    void setUp() {
        response = new IngestionResponseDTO();
        response.setRawId(1L);
        response.setClaimId("CLM-001");
        response.setFeedId(1L);
        response.setFeedType("CLAIM");
        response.setPayloadJson("{\"amount\":5000}");
        response.setIngestedDate(LocalDateTime.now());
    }

    @Test
    void ingestClaim_returns201() throws Exception {
        IngestionRequestDTO req = new IngestionRequestDTO();
        req.setClaimId("CLM-001");
        req.setFeedId(1L);
        req.setPayloadJson("{\"amount\":5000}");

        when(ingestionService.ingestClaim(any())).thenReturn(response);

        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").value("CLM-001"));
    }

    @Test
    void ingestClaim_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestClaim_feedNotFound_returns404() throws Exception {
        IngestionRequestDTO req = new IngestionRequestDTO();
        req.setClaimId("CLM-001");
        req.setFeedId(99L);
        req.setPayloadJson("{\"amount\":5000}");

        when(ingestionService.ingestClaim(any()))
                .thenThrow(new ResourceNotFoundException("Feed not found"));

        mockMvc.perform(post("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllRawClaims_returns200() throws Exception {
        when(ingestionService.getAllRawClaims()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ingest/raw-claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRawClaimsByClaimId_returns200() throws Exception {
        when(ingestionService.getRawClaimsByClaimId("CLM-001")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ingest/raw-claims/CLM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value("CLM-001"));
    }

    @Test
    void getRawClaimsByFeedId_returns200() throws Exception {
        when(ingestionService.getRawClaimsByFeedId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ingest/raw-claims/feed/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRawClaimsByFeedId_notFound_returns404() throws Exception {
        when(ingestionService.getRawClaimsByFeedId(99L))
                .thenThrow(new ResourceNotFoundException("Feed not found"));

        mockMvc.perform(get("/api/ingest/raw-claims/feed/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRawClaim_returns204() throws Exception {
        doNothing().when(ingestionService).deleteRawClaim(1L);

        mockMvc.perform(delete("/api/ingest/raw-claims/1"))
                .andExpect(status().isNoContent());
    }
}
