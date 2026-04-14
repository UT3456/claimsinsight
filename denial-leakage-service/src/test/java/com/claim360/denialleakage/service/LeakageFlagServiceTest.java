package com.claim360.denialleakage.service;

import com.claim360.denialleakage.dto.LeakageFlagRequest;
import com.claim360.denialleakage.dto.LeakageFlagResponse;
import com.claim360.denialleakage.entity.LeakageFlag;
import com.claim360.denialleakage.enums.LeakageType;
import com.claim360.denialleakage.exception.ResourceNotFoundException;
import com.claim360.denialleakage.repository.LeakageFlagRepository;
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
@DisplayName("LeakageFlag Service Tests")
class LeakageFlagServiceTest {

    @Mock
    private LeakageFlagRepository leakageFlagRepository;

    @InjectMocks
    private LeakageFlagServiceImpl leakageFlagService;

    private LeakageFlag flag;
    private LeakageFlagRequest request;

    @BeforeEach
    void setUp() {
        flag = new LeakageFlag(
                1L,
                "CLM-001",
                LeakageType.Overpayment,
                5000.0,
                LocalDate.of(2024, 1, 15)
        );

        request = new LeakageFlagRequest();
        request.setClaimId("CLM-001");
        request.setLeakageType(LeakageType.Overpayment);
        request.setEstimatedLoss(5000.0);
        request.setIdentifiedDate(LocalDate.of(2024, 1, 15));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ID not found")
    void getLeakageFlagById_WhenNotExists_ShouldThrowException() {
        when(leakageFlagRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leakageFlagService.getLeakageFlagById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LeakageFlag")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should return empty when no LeakageFlags exist")
    void getAllLeakageFlags_WhenEmpty_ShouldReturnEmptyList() {
        when(leakageFlagRepository.findAll())
                .thenReturn(List.of());

        List<LeakageFlagResponse> results =
                leakageFlagService.getAllLeakageFlags();

        assertThat(results).isEmpty();
    }


    @Test
    @DisplayName("Should throw exception when ClaimId not found")
    void getLeakageFlagsByClaimId_WhenNotExists_ShouldThrowException() {
        when(leakageFlagRepository.findByClaimId("CLM-999"))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                leakageFlagService.getLeakageFlagsByClaimId("CLM-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CLM-999");
    }



    @Test
    @DisplayName("Should throw exception when LeakageType not found")
    void getLeakageFlagsByLeakageType_WhenNotExists_ShouldThrowException() {
        when(leakageFlagRepository
                .findByLeakageType(LeakageType.Error))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                leakageFlagService
                        .getLeakageFlagsByLeakageType(LeakageType.Error))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    @DisplayName("Should throw exception when no flags above threshold")
    void getLeakageFlagsByEstimatedLoss_WhenNotExists_ShouldThrowException() {
        when(leakageFlagRepository
                .findByEstimatedLossGreaterThanEqual(99999.0))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                leakageFlagService
                        .getLeakageFlagsByEstimatedLoss(99999.0))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    @DisplayName("Should throw exception when updating non-existent LeakageFlag")
    void updateLeakageFlag_WhenNotExists_ShouldThrowException() {
        when(leakageFlagRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leakageFlagService.updateLeakageFlag(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LeakageFlag")
                .hasMessageContaining("99");
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete LeakageFlag when ID exists")
    void deleteLeakageFlag_WhenExists_ShouldDelete() {
        when(leakageFlagRepository.findById(1L))
                .thenReturn(Optional.of(flag));
        doNothing().when(leakageFlagRepository).delete(flag);

        assertThatCode(() ->
                leakageFlagService.deleteLeakageFlag(1L))
                .doesNotThrowAnyException();

        verify(leakageFlagRepository, times(1)).delete(flag);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent LeakageFlag")
    void deleteLeakageFlag_WhenNotExists_ShouldThrowException() {
        when(leakageFlagRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                leakageFlagService.deleteLeakageFlag(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LeakageFlag")
                .hasMessageContaining("99");
    }
}