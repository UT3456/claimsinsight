package com.claims.controller;

import com.claims.dto.response.AgingRecordResponse;
import com.claims.enums.AgingBucket;
import com.claims.service.AgingRecordServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AgingRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgingRecordServiceImpl agingRecordServiceImpl;

    private ObjectMapper objectMapper;
    private AgingRecordResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        response = new AgingRecordResponse(1L, "CLAIM123", 45, AgingBucket.BUCKET_31_60);
    }



    @Test
    void getAgingRecordsByBucket_Returns200() throws Exception {
        when(agingRecordServiceImpl.getAgingRecordsByBucket(AgingBucket.BUCKET_31_60))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/aging/bucket/BUCKET_31_60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value("CLAIM123"));
    }

    @Test
    void getPortfolioAgingHealth_Returns200() throws Exception {
        Map<String, Object> health = Map.of("totalClaims", 100L, "averageAgingDays", 35.5);
        when(agingRecordServiceImpl.getPortfolioAgingHealth()).thenReturn(health);

        mockMvc.perform(get("/api/aging/analytics/portfolio-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClaims").value(100));
    }
}