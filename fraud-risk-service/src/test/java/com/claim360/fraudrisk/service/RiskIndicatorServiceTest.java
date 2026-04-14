package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.dto.RiskIndicatorRequest;
import com.claim360.fraudrisk.dto.RiskIndicatorResponse;
import com.claim360.fraudrisk.entity.RiskIndicator;
import com.claim360.fraudrisk.enums.IndicatorType;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.repository.RiskIndicatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskIndicator Service Tests")
class RiskIndicatorServiceTest {

    @Mock
    private RiskIndicatorRepository riskIndicatorRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private RiskIndicatorServiceImpl riskIndicatorService;

    private RiskIndicator indicator;
    private RiskIndicatorRequest request;
    private RiskIndicatorResponse response;

    @BeforeEach
    void setUp() {
        indicator = new RiskIndicator(
                1L, "CLM-001", IndicatorType.HighCost,
                "HIGH", LocalDate.of(2024, 1, 15)
        );

        request = new RiskIndicatorRequest();
        request.setClaimId("CLM-001");
        request.setIndicatorType(IndicatorType.HighCost);
        request.setSeverity("HIGH");
        request.setTriggeredDate(LocalDate.of(2024, 1, 15));

        response = new RiskIndicatorResponse(
                1L, "CLM-001", IndicatorType.HighCost,
                "HIGH", LocalDate.of(2024, 1, 15)
        );
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create RiskIndicator and return response")
    void createRiskIndicator_ShouldReturnResponse() {
        when(modelMapper.map(request, RiskIndicator.class)).thenReturn(indicator);
        when(riskIndicatorRepository.save(indicator)).thenReturn(indicator);
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        RiskIndicatorResponse result = riskIndicatorService.createRiskIndicator(request);

        assertThat(result).isNotNull();
        assertThat(result.getClaimId()).isEqualTo("CLM-001");
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getIndicatorType()).isEqualTo(IndicatorType.HighCost);
        verify(riskIndicatorRepository, times(1)).save(indicator);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return RiskIndicator when ID exists")
    void getRiskIndicatorById_WhenExists_ShouldReturnResponse() {
        when(riskIndicatorRepository.findById(1L)).thenReturn(Optional.of(indicator));
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        RiskIndicatorResponse result = riskIndicatorService.getRiskIndicatorById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getIndicatorId()).isEqualTo(1L);
        assertThat(result.getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ID not found")
    void getRiskIndicatorById_WhenNotExists_ShouldThrowException() {
        when(riskIndicatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskIndicatorService.getRiskIndicatorById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RiskIndicator");
    }

    @Test
    @DisplayName("Should return all RiskIndicators")
    void getAllRiskIndicators_ShouldReturnList() {
        when(riskIndicatorRepository.findAll()).thenReturn(List.of(indicator));
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        List<RiskIndicatorResponse> results = riskIndicatorService.getAllRiskIndicators();

        assertThat(results).isNotEmpty();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    @DisplayName("Should return RiskIndicators by claimId")
    void getRiskIndicatorsByClaimId_WhenExists_ShouldReturnList() {
        when(riskIndicatorRepository.findByClaimId("CLM-001")).thenReturn(List.of(indicator));
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        List<RiskIndicatorResponse> results =
                riskIndicatorService.getRiskIndicatorsByClaimId("CLM-001");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    @DisplayName("Should throw exception when claimId not found")
    void getRiskIndicatorsByClaimId_WhenNotExists_ShouldThrowException() {
        when(riskIndicatorRepository.findByClaimId("CLM-999")).thenReturn(List.of());

        assertThatThrownBy(() -> riskIndicatorService.getRiskIndicatorsByClaimId("CLM-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CLM-999");
    }

    @Test
    @DisplayName("Should return RiskIndicators by indicator type")
    void getRiskIndicatorsByType_WhenExists_ShouldReturnList() {
        when(riskIndicatorRepository.findByIndicatorType(IndicatorType.HighCost))
                .thenReturn(List.of(indicator));
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        List<RiskIndicatorResponse> results =
                riskIndicatorService.getRiskIndicatorsByType(IndicatorType.HighCost);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIndicatorType()).isEqualTo(IndicatorType.HighCost);
    }

    @Test
    @DisplayName("Should return RiskIndicators by severity")
    void getRiskIndicatorsBySeverity_WhenExists_ShouldReturnList() {
        when(riskIndicatorRepository.findBySeverity("HIGH")).thenReturn(List.of(indicator));
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        List<RiskIndicatorResponse> results =
                riskIndicatorService.getRiskIndicatorsBySeverity("HIGH");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeverity()).isEqualTo("HIGH");
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update RiskIndicator when ID exists")
    void updateRiskIndicator_WhenExists_ShouldReturnUpdatedResponse() {
        when(riskIndicatorRepository.findById(1L)).thenReturn(Optional.of(indicator));
        doNothing().when(modelMapper).map(request, indicator);
        when(riskIndicatorRepository.save(indicator)).thenReturn(indicator);
        when(modelMapper.map(indicator, RiskIndicatorResponse.class)).thenReturn(response);

        RiskIndicatorResponse result =
                riskIndicatorService.updateRiskIndicator(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getClaimId()).isEqualTo("CLM-001");
        verify(riskIndicatorRepository, times(1)).save(indicator);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existing RiskIndicator")
    void updateRiskIndicator_WhenNotExists_ShouldThrowException() {
        when(riskIndicatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskIndicatorService.updateRiskIndicator(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RiskIndicator");
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete RiskIndicator when ID exists")
    void deleteRiskIndicator_WhenExists_ShouldDelete() {
        when(riskIndicatorRepository.findById(1L)).thenReturn(Optional.of(indicator));
        doNothing().when(riskIndicatorRepository).delete(indicator);

        assertThatCode(() -> riskIndicatorService.deleteRiskIndicator(1L))
                .doesNotThrowAnyException();

        verify(riskIndicatorRepository, times(1)).delete(indicator);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existing RiskIndicator")
    void deleteRiskIndicator_WhenNotExists_ShouldThrowException() {
        when(riskIndicatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskIndicatorService.deleteRiskIndicator(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RiskIndicator");
    }
}