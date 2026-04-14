package com.claiminsight.ingestion.controller;

import com.claiminsight.ingestion.dto.DataFeedRequestDTO;
import com.claiminsight.ingestion.dto.DataFeedResponseDTO;
import com.claiminsight.ingestion.dto.FeedStatusUpdateDTO;
import com.claiminsight.ingestion.exception.GlobalExceptionHandler;
import com.claiminsight.ingestion.exception.ResourceNotFoundException;
import com.claiminsight.ingestion.service.DataFeedService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataFeedController.class)
@Import(GlobalExceptionHandler.class)
class DataFeedControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DataFeedService dataFeedService;
    @Autowired ObjectMapper objectMapper;

    private DataFeedResponseDTO response;

    @BeforeEach
    void setUp() {
        response = new DataFeedResponseDTO();
        response.setFeedId(1L);
        response.setFeedType("CLAIM");
        response.setSourceSystem("ClaimsPro v3");
        response.setStatus("ACTIVE");
        response.setCreatedDate(LocalDateTime.now());
    }

    @Test
    void createFeed_returns201() throws Exception {
        DataFeedRequestDTO req = new DataFeedRequestDTO();
        req.setFeedType("CLAIM");
        req.setSourceSystem("ClaimsPro v3");
        req.setStatus("ACTIVE");

        when(dataFeedService.createFeed(any())).thenReturn(response);

        mockMvc.perform(post("/api/feeds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedId").value(1));
    }

    @Test
    void createFeed_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/feeds")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllFeeds_returns200() throws Exception {
        when(dataFeedService.getAllFeeds()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getFeedById_returns200() throws Exception {
        when(dataFeedService.getFeedById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/feeds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedId").value(1));
    }

    @Test
    void getFeedById_notFound_returns404() throws Exception {
        when(dataFeedService.getFeedById(99L))
                .thenThrow(new ResourceNotFoundException("DataFeed with ID 99 not found"));

        mockMvc.perform(get("/api/feeds/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_returns200() throws Exception {
        FeedStatusUpdateDTO dto = new FeedStatusUpdateDTO();
        dto.setStatus("INACTIVE");

        DataFeedResponseDTO updated = new DataFeedResponseDTO();
        updated.setStatus("INACTIVE");

        when(dataFeedService.updateStatus(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/feeds/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deleteFeed_returns204() throws Exception {
        doNothing().when(dataFeedService).deleteFeed(1L);

        mockMvc.perform(delete("/api/feeds/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteFeed_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("not found"))
                .when(dataFeedService).deleteFeed(99L);

        mockMvc.perform(delete("/api/feeds/99"))
                .andExpect(status().isNotFound());
    }
}
