package com.claim360.denialleakage.service;

import com.claim360.denialleakage.dto.DenialPatternRequest;
import com.claim360.denialleakage.dto.DenialPatternResponse;
import com.claim360.denialleakage.entity.DenialPattern;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.repository.DenialPatternRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DenialPattern Service Tests")
class DenialPatternServiceTest {

    @Mock
    private DenialPatternRepository denialPatternRepository;

    @InjectMocks
    private DenialPatternServiceImpl denialPatternService;

    private DenialPattern pattern;
    private DenialPatternRequest request;

    @BeforeEach
    void setUp() {
        // Setup test entity
        pattern = new DenialPattern(
                1L,
                "CLM-001",
                "CO-4",
                "Service not covered",
                LocalDate.of(2024, 1, 15)
        );

        // Setup test request
        request = new DenialPatternRequest();
        request.setClaimId("CLM-001");
        request.setDenialCode("CO-4");
        request.setReason("Service not covered");
        request.setOccurrenceDate(LocalDate.of(2024, 1, 15));
    }

    // ── Create ───────────────────────────────────────────────────────────────


    // ── Read ─────────────────────────────────────────────────────────────────



    @Test
    @DisplayName("Should throw ResourceNotFoundException when ID not found")
    void getDenialPatternById_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                denialPatternService.getDenialPatternById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DenialPattern")
                .hasMessageContaining("99");
    }


    @Test
    @DisplayName("Should return empty when no DenialPatterns exist")
    void getAllDenialPatterns_WhenEmpty_ShouldReturnEmptyList() {
        when(denialPatternRepository.findAll())
                .thenReturn(List.of());

        List<DenialPatternResponse> results =
                denialPatternService.getAllDenialPatterns();

        assertThat(results).isEmpty();
    }


    @Test
    @DisplayName("Should throw exception when ClaimId not found")
    void getDenialPatternsByClaimId_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository.findByClaimId("CLM-999"))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                denialPatternService.getDenialPatternsByClaimId("CLM-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CLM-999");
    }


    @Test
    @DisplayName("Should throw exception when DenialCode not found")
    void getDenialPatternsByDenialCode_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository.findByDenialCode("XX-99"))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                denialPatternService.getDenialPatternsByDenialCode("XX-99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("XX-99");
    }

    @Test
    @DisplayName("Should throw exception when keyword not found")
    void getDenialPatternsByReasonKeyword_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository
                .findByReasonContainingIgnoreCase("xyz"))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                denialPatternService
                        .getDenialPatternsByReasonKeyword("xyz"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when ClaimId and DenialCode not found")
    void getDenialPatternsByClaimIdAndDenialCode_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository
                .findByClaimIdAndDenialCode("CLM-999", "XX-99"))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                denialPatternService
                        .getDenialPatternsByClaimIdAndDenialCode(
                                "CLM-999", "XX-99"))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    @DisplayName("Should throw exception when updating non-existent DenialPattern")
    void updateDenialPattern_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                denialPatternService.updateDenialPattern(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DenialPattern")
                .hasMessageContaining("99");
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete DenialPattern when ID exists")
    void deleteDenialPattern_WhenExists_ShouldDelete() {
        when(denialPatternRepository.findById(1L))
                .thenReturn(Optional.of(pattern));
        doNothing().when(denialPatternRepository).delete(pattern);

        assertThatCode(() ->
                denialPatternService.deleteDenialPattern(1L))
                .doesNotThrowAnyException();

        verify(denialPatternRepository, times(1)).delete(pattern);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent DenialPattern")
    void deleteDenialPattern_WhenNotExists_ShouldThrowException() {
        when(denialPatternRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                denialPatternService.deleteDenialPattern(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DenialPattern")
                .hasMessageContaining("99");
    }
}