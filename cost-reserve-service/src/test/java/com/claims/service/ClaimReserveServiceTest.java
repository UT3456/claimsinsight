package com.claims.service;

import com.claims.dto.request.ClaimReserveRequest;
import com.claims.dto.response.ClaimReserveResponse;
import com.claims.entity.ClaimReserve;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.ClaimReserveRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimReserveServiceTest {

    @Mock
    private ClaimReserveRepository claimReserveRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ClaimReserveServiceImpl claimReserveService;

    private ClaimReserve claimReserve;
    private ClaimReserveRequest request;
    private ClaimReserveResponse response;

    @BeforeEach
    void setUp() {
        claimReserve = new ClaimReserve(1L, "CLM001",
                new BigDecimal("15000.00"), LocalDate.of(2024, 1, 10));

        request = new ClaimReserveRequest("CLM001",
                new BigDecimal("15000.00"), LocalDate.of(2024, 1, 10));

        response = new ClaimReserveResponse(1L, "CLM001",
                new BigDecimal("15000.00"), LocalDate.of(2024, 1, 10));
    }

    @Test
    void testCreateClaimReserve_success() {
        when(modelMapper.map(request, ClaimReserve.class)).thenReturn(claimReserve);
        when(claimReserveRepository.save(claimReserve)).thenReturn(claimReserve);
        when(modelMapper.map(claimReserve, ClaimReserveResponse.class)).thenReturn(response);

        ClaimReserveResponse result = claimReserveService.createClaimReserve(request);

        assertThat(result.getReserveId()).isEqualTo(1L);
        assertThat(result.getClaimId()).isEqualTo("CLM001");
        verify(claimReserveRepository).save(claimReserve);
    }

    @Test
    void testGetClaimReserveById_success() {
        when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(claimReserve));
        when(modelMapper.map(claimReserve, ClaimReserveResponse.class)).thenReturn(response);

        ClaimReserveResponse result = claimReserveService.getClaimReserveById(1L);

        assertThat(result.getReserveId()).isEqualTo(1L);
    }

    @Test
    void testGetClaimReserveById_notFound_throwsException() {
        when(claimReserveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimReserveService.getClaimReserveById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetAllClaimReserves_success() {
        when(claimReserveRepository.findAll()).thenReturn(List.of(claimReserve));
        when(modelMapper.map(claimReserve, ClaimReserveResponse.class)).thenReturn(response);

        List<ClaimReserveResponse> result = claimReserveService.getAllClaimReserves();

        assertThat(result).hasSize(1);
    }


    @Test
    void testUpdateClaimReserve_notFound_throwsException() {
        when(claimReserveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimReserveService.updateClaimReserve(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testDeleteClaimReserve_success() {
        when(claimReserveRepository.findById(1L)).thenReturn(Optional.of(claimReserve));

        claimReserveService.deleteClaimReserve(1L);

        verify(claimReserveRepository).delete(claimReserve);
    }

    @Test
    void testDeleteClaimReserve_notFound_throwsException() {
        when(claimReserveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimReserveService.deleteClaimReserve(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReservesByClaimId_success() {
        when(claimReserveRepository.findByClaimId("CLM001")).thenReturn(List.of(claimReserve));
        when(modelMapper.map(claimReserve, ClaimReserveResponse.class)).thenReturn(response);

        List<ClaimReserveResponse> result = claimReserveService.getReservesByClaimId("CLM001");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetReservesByClaimId_notFound_throwsException() {
        when(claimReserveRepository.findByClaimId("CLM999")).thenReturn(List.of());

        assertThatThrownBy(() -> claimReserveService.getReservesByClaimId("CLM999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReservesByDateRange_invalidDates_throwsException() {
        assertThatThrownBy(() -> claimReserveService.getReservesByDateRange(
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void testGetLatestReserveForClaim_success() {
        when(claimReserveRepository.findLatestReserveByClaimId("CLM001"))
                .thenReturn(List.of(claimReserve));
        when(modelMapper.map(claimReserve, ClaimReserveResponse.class)).thenReturn(response);

        ClaimReserveResponse result = claimReserveService.getLatestReserveForClaim("CLM001");

        assertThat(result.getClaimId()).isEqualTo("CLM001");
    }

    @Test
    void testGetLatestReserveForClaim_notFound_throwsException() {
        when(claimReserveRepository.findLatestReserveByClaimId("CLM999")).thenReturn(List.of());

        assertThatThrownBy(() -> claimReserveService.getLatestReserveForClaim("CLM999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetTotalReserveAmount_success() {
        when(claimReserveRepository.getTotalReserveAmount())
                .thenReturn(new BigDecimal("50000.00"));

        BigDecimal result = claimReserveService.getTotalReserveAmount();

        assertThat(result).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void testGetReserveAdequacy_adequate() {
        when(claimReserveRepository.findLatestReserveByClaimId("CLM001"))
                .thenReturn(List.of(claimReserve));

        Map<String, Object> result = claimReserveService.getReserveAdequacyForClaim(
                "CLM001", new BigDecimal("12000.00"));

        assertThat(result.get("adequacyStatus")).isEqualTo("ADEQUATE");
    }

    @Test
    void testGetReserveAdequacy_underReserved() {
        when(claimReserveRepository.findLatestReserveByClaimId("CLM001"))
                .thenReturn(List.of(claimReserve));

        Map<String, Object> result = claimReserveService.getReserveAdequacyForClaim(
                "CLM001", new BigDecimal("20000.00"));

        assertThat(result.get("adequacyStatus")).isEqualTo("UNDER_RESERVED");
    }
}

