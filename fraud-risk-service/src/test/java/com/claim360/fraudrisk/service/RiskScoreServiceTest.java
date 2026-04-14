package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.dto.RiskScoreRequest;
import com.claim360.fraudrisk.dto.RiskScoreResponse;
import com.claim360.fraudrisk.entity.RiskScore;
import com.claim360.fraudrisk.exception.ResourceNotFoundException;
import com.claim360.fraudrisk.repository.RiskScoreRepository;
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
@DisplayName("RiskScore Service Tests")
class RiskScoreServiceTest {

    @Mock
    private RiskScoreRepository riskScoreRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private RiskScoreServiceImpl riskScoreService;

    private RiskScore score;
    private RiskScoreRequest request;
    private RiskScoreResponse response;

    @BeforeEach
    void setUp() {
        score = new RiskScore(
                1L, "CLM-001", 85.5,
                LocalDate.of(2024, 1, 15)
        );

        request = new RiskScoreRequest();
        request.setClaimId("CLM-001");
        request.setScoreValue(85.5);
        request.setComputedDate(LocalDate.of(2024, 1, 15));

        response = new RiskScoreResponse(
                1L, "CLM-001", 85.5,
                LocalDate.of(2024, 1, 15)
        );
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create RiskScore and return response")
    void createRiskScore_ShouldReturnResponse() {
        when(modelMapper.map(request, RiskScore.class)).thenReturn(score);
        when(riskScoreRepository.save(score)).thenReturn(score);
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        RiskScoreResponse result = riskScoreService.createRiskScore(request);

        assertThat(result).isNotNull();
        assertThat(result.getClaimId()).isEqualTo("CLM-001");
        assertThat(result.getScoreValue()).isEqualTo(85.5);
        verify(riskScoreRepository, times(1)).save(score);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return RiskScore when ID exists")
    void getRiskScoreById_WhenExists_ShouldReturnResponse() {
        when(riskScoreRepository.findById(1L)).thenReturn(Optional.of(score));
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        RiskScoreResponse result = riskScoreService.getRiskScoreById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getScoreId()).isEqualTo(1L);
        assertThat(result.getScoreValue()).isEqualTo(85.5);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ID not found")
    void getRiskScoreById_WhenNotExists_ShouldThrowException() {
        when(riskScoreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskScoreService.getRiskScoreById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RiskScore");
    }

    @Test
    @DisplayName("Should return all RiskScores")
    void getAllRiskScores_ShouldReturnList() {
        when(riskScoreRepository.findAll()).thenReturn(List.of(score));
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        List<RiskScoreResponse> results = riskScoreService.getAllRiskScores();

        assertThat(results).isNotEmpty();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    @DisplayName("Should return RiskScores by claimId")
    void getRiskScoresByClaimId_WhenExists_ShouldReturnList() {
        when(riskScoreRepository.findByClaimId("CLM-001")).thenReturn(List.of(score));
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        List<RiskScoreResponse> results =
                riskScoreService.getRiskScoresByClaimId("CLM-001");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    @DisplayName("Should throw exception when claimId not found")
    void getRiskScoresByClaimId_WhenNotExists_ShouldThrowException() {
        when(riskScoreRepository.findByClaimId("CLM-999")).thenReturn(List.of());

        assertThatThrownBy(() -> riskScoreService.getRiskScoresByClaimId("CLM-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CLM-999");
    }

    @Test
    @DisplayName("Should return latest RiskScore for claimId")
    void getLatestRiskScoreByClaimId_WhenExists_ShouldReturnResponse() {
        when(riskScoreRepository.findTopByClaimIdOrderByComputedDateDesc("CLM-001"))
                .thenReturn(Optional.of(score));
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        RiskScoreResponse result =
                riskScoreService.getLatestRiskScoreByClaimId("CLM-001");

        assertThat(result).isNotNull();
        assertThat(result.getScoreValue()).isEqualTo(85.5);
    }

    @Test
    @DisplayName("Should throw exception when latest score not found")
    void getLatestRiskScoreByClaimId_WhenNotExists_ShouldThrowException() {
        when(riskScoreRepository.findTopByClaimIdOrderByComputedDateDesc("CLM-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskScoreService.getLatestRiskScoreByClaimId("CLM-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CLM-999");
    }

    @Test
    @DisplayName("Should return RiskScores above threshold")
    void getRiskScoresAboveThreshold_WhenExists_ShouldReturnList() {
        when(riskScoreRepository.findByScoreValueGreaterThanEqual(80.0))
                .thenReturn(List.of(score));
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        List<RiskScoreResponse> results =
                riskScoreService.getRiskScoresAboveThreshold(80.0);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScoreValue()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("Should throw exception when no scores above threshold")
    void getRiskScoresAboveThreshold_WhenNoneFound_ShouldThrowException() {
        when(riskScoreRepository.findByScoreValueGreaterThanEqual(99.0))
                .thenReturn(List.of());

        assertThatThrownBy(() -> riskScoreService.getRiskScoresAboveThreshold(99.0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update RiskScore when ID exists")
    void updateRiskScore_WhenExists_ShouldReturnUpdatedResponse() {
        when(riskScoreRepository.findById(1L)).thenReturn(Optional.of(score));
        doNothing().when(modelMapper).map(request, score);
        when(riskScoreRepository.save(score)).thenReturn(score);
        when(modelMapper.map(score, RiskScoreResponse.class)).thenReturn(response);

        RiskScoreResponse result = riskScoreService.updateRiskScore(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.getScoreValue()).isEqualTo(85.5);
        verify(riskScoreRepository, times(1)).save(score);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existing RiskScore")
    void updateRiskScore_WhenNotExists_ShouldThrowException() {
        when(riskScoreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskScoreService.updateRiskScore(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RiskScore");
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete RiskScore when ID exists")
    void deleteRiskScore_WhenExists_ShouldDelete() {
        when(riskScoreRepository.findById(1L)).thenReturn(Optional.of(score));
        doNothing().when(riskScoreRepository).delete(score);

        assertThatCode(() -> riskScoreService.deleteRiskScore(1L))
                .doesNotThrowAnyException();

        verify(riskScoreRepository, times(1)).delete(score);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existing RiskScore")
    void deleteRiskScore_WhenNotExists_ShouldThrowException() {
        when(riskScoreRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskScoreService.deleteRiskScore(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RiskScore");
    }
}