


package com.claims.service;

import com.claims.dto.request.ClaimCostRequest;
import com.claims.dto.response.ClaimCostResponse;
import com.claims.entity.ClaimCost;
import com.claims.enums.CostType;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.ClaimCostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimCostServiceTest {

    @Mock
    private ClaimCostRepository claimCostRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ClaimCostServiceImpl claimCostService;

    private ClaimCost claimCost;
    private ClaimCostRequest request;
    private ClaimCostResponse response;

    @BeforeEach
    void setUp() {
        claimCost = new ClaimCost(1L, "CLM001", CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));

        request = new ClaimCostRequest("CLM001", CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));

        response = new ClaimCostResponse(1L, "CLM001", CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15));
    }

    @Test
    void testCreateClaimCost_success() {
        when(modelMapper.map(request, ClaimCost.class)).thenReturn(claimCost);
        when(claimCostRepository.save(claimCost)).thenReturn(claimCost);
        when(modelMapper.map(claimCost, ClaimCostResponse.class)).thenReturn(response);

        ClaimCostResponse result = claimCostService.createClaimCost(request);

        assertThat(result.getCostId()).isEqualTo(1L);
        assertThat(result.getClaimId()).isEqualTo("CLM001");
        assertThat(result.getCostType()).isEqualTo(CostType.MEDICAL);
        verify(claimCostRepository).save(claimCost);
    }

    @Test
    void testGetClaimCostById_success() {
        when(claimCostRepository.findById(1L)).thenReturn(Optional.of(claimCost));
        when(modelMapper.map(claimCost, ClaimCostResponse.class)).thenReturn(response);

        ClaimCostResponse result = claimCostService.getClaimCostById(1L);

        assertThat(result.getCostId()).isEqualTo(1L);
    }

    @Test
    void testGetClaimCostById_notFound_throwsException() {
        when(claimCostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimCostService.getClaimCostById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetAllClaimCosts_success() {
        when(claimCostRepository.findAll()).thenReturn(List.of(claimCost));
        when(modelMapper.map(claimCost, ClaimCostResponse.class)).thenReturn(response);

        List<ClaimCostResponse> result = claimCostService.getAllClaimCosts();

        assertThat(result).hasSize(1);
    }



    @Test
    void testUpdateClaimCost_notFound_throwsException() {
        when(claimCostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimCostService.updateClaimCost(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testDeleteClaimCost_success() {
        when(claimCostRepository.findById(1L)).thenReturn(Optional.of(claimCost));

        claimCostService.deleteClaimCost(1L);

        verify(claimCostRepository).delete(claimCost);
    }

    @Test
    void testDeleteClaimCost_notFound_throwsException() {
        when(claimCostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimCostService.deleteClaimCost(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetCostsByClaimId_success() {
        when(claimCostRepository.findByClaimId("CLM001")).thenReturn(List.of(claimCost));
        when(modelMapper.map(claimCost, ClaimCostResponse.class)).thenReturn(response);

        List<ClaimCostResponse> result = claimCostService.getCostsByClaimId("CLM001");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetCostsByClaimId_notFound_throwsException() {
        when(claimCostRepository.findByClaimId("CLM999")).thenReturn(List.of());

        assertThatThrownBy(() -> claimCostService.getCostsByClaimId("CLM999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetCostsByCostType_success() {
        when(claimCostRepository.findByCostType(CostType.MEDICAL)).thenReturn(List.of(claimCost));
        when(modelMapper.map(claimCost, ClaimCostResponse.class)).thenReturn(response);

        List<ClaimCostResponse> result = claimCostService.getCostsByCostType(CostType.MEDICAL);

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetCostsByDateRange_invalidDates_throwsException() {
        assertThatThrownBy(() -> claimCostService.getCostsByDateRange(
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void testGetTotalCostByClaimId_success() {
        when(claimCostRepository.findByClaimId("CLM001")).thenReturn(List.of(claimCost));
        when(claimCostRepository.getTotalAmountByClaimId("CLM001"))
                .thenReturn(new BigDecimal("5000.00"));

        BigDecimal result = claimCostService.getTotalCostByClaimId("CLM001");

        assertThat(result).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void testGetHighestCostClaim_success() {
        ClaimCost cost2 = new ClaimCost(2L, "CLM002", CostType.LEGAL,
                new BigDecimal("8000.00"), LocalDate.of(2024, 2, 1));

        when(claimCostRepository.findAll()).thenReturn(List.of(claimCost, cost2));

        Map<String, Object> result = claimCostService.getHighestCostClaim();

        assertThat(result.get("claimId")).isEqualTo("CLM002");
    }

    @Test
    void testGetHighestCostClaim_noCosts_throwsException() {
        when(claimCostRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> claimCostService.getHighestCostClaim())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}