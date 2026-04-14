package com.claiminsight.metrics.service;

import com.claiminsight.metrics.dto.ClaimKpiRequestDTO;
import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.exception.InvalidMetricException;
import com.claiminsight.metrics.exception.ResourceNotFoundException;
import com.claiminsight.metrics.mapper.ClaimKpiMapper;
import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.MetricName;
import com.claiminsight.metrics.repository.ClaimKpiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimKpiServiceTest {

    @Mock ClaimKpiRepository claimKpiRepository;
    @Mock ClaimKpiMapper claimKpiMapper;
    @InjectMocks ClaimKpiService claimKpiService;

    private ClaimKPI kpi;
    private ClaimKpiResponseDTO dto;

    @BeforeEach
    void setUp() {
        kpi = new ClaimKPI();
        kpi.setKpiId(1L);
        kpi.setClaimId("CLM-001");
        kpi.setMetricName(MetricName.TAT);
        kpi.setMetricValue(new BigDecimal("5.0"));
        kpi.setMetricDate(LocalDate.of(2026, 1, 15));

        dto = new ClaimKpiResponseDTO();
        dto.setKpiId(1L);
        dto.setClaimId("CLM-001");
        dto.setMetricName("TAT");
        dto.setMetricValue(new BigDecimal("5.0"));
        dto.setMetricDate(LocalDate.of(2026, 1, 15));
    }

    @Test
    void createKpi_success() {
        ClaimKpiRequestDTO req = new ClaimKpiRequestDTO();
        req.setClaimId("CLM-001");
        req.setMetricName("TAT");
        req.setMetricValue(new BigDecimal("5.0"));
        req.setMetricDate(LocalDate.now());

        when(claimKpiMapper.toEntity(any(), any())).thenReturn(kpi);
        when(claimKpiRepository.save(any())).thenReturn(kpi);
        when(claimKpiMapper.toResponseDTO(any())).thenReturn(dto);

        assertNotNull(claimKpiService.createKpi(req));
    }

    @Test
    void createKpi_invalidMetricName_throwsException() {
        ClaimKpiRequestDTO req = new ClaimKpiRequestDTO();
        req.setClaimId("CLM-001");
        req.setMetricName("INVALID");
        req.setMetricValue(new BigDecimal("5.0"));
        req.setMetricDate(LocalDate.now());

        assertThrows(InvalidMetricException.class, () -> claimKpiService.createKpi(req));
        verify(claimKpiRepository, never()).save(any());
    }

    @Test
    void getAllKpis_returnsList() {
        when(claimKpiRepository.findAll()).thenReturn(List.of(kpi));
        when(claimKpiMapper.toResponseDTO(kpi)).thenReturn(dto);

        assertEquals(1, claimKpiService.getAllKpis().size());
    }

    @Test
    void getKpisByClaimId_returnsMappedList() {
        when(claimKpiRepository.findByClaimId("CLM-001")).thenReturn(List.of(kpi));
        when(claimKpiMapper.toResponseDTO(kpi)).thenReturn(dto);

        List<ClaimKpiResponseDTO> result = claimKpiService.getKpisByClaimId("CLM-001");

        assertEquals(1, result.size());
        assertEquals("CLM-001", result.get(0).getClaimId());
    }

    @Test
    void getKpisByMetricName_validName_returnsMappedList() {
        when(claimKpiRepository.findByMetricName(MetricName.TAT)).thenReturn(List.of(kpi));
        when(claimKpiMapper.toResponseDTO(kpi)).thenReturn(dto);

        List<ClaimKpiResponseDTO> result = claimKpiService.getKpisByMetricName("tat");

        assertEquals(1, result.size());
        verify(claimKpiRepository).findByMetricName(MetricName.TAT);
    }

    @Test
    void getKpisByClaimIdAndMetricName_returnsMappedList() {
        when(claimKpiRepository.findByClaimIdAndMetricName("CLM-001", MetricName.TAT)).thenReturn(List.of(kpi));
        when(claimKpiMapper.toResponseDTO(kpi)).thenReturn(dto);

        List<ClaimKpiResponseDTO> result = claimKpiService.getKpisByClaimIdAndMetricName("CLM-001", "tat");

        assertEquals(1, result.size());
        verify(claimKpiRepository).findByClaimIdAndMetricName("CLM-001", MetricName.TAT);
    }

    @Test
    void getKpisByDateRange_returnsMappedList() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(claimKpiRepository.findByMetricDateBetween(start, end)).thenReturn(List.of(kpi));
        when(claimKpiMapper.toResponseDTO(kpi)).thenReturn(dto);

        List<ClaimKpiResponseDTO> result = claimKpiService.getKpisByDateRange(start, end);

        assertEquals(1, result.size());
        verify(claimKpiRepository).findByMetricDateBetween(start, end);
    }

    @Test
    void getKpisByClaimIdAndMetricName_invalidName_throwsException() {
        assertThrows(InvalidMetricException.class,
                () -> claimKpiService.getKpisByClaimIdAndMetricName("CLM-001", "WRONG"));
    }

    @Test
    void getKpiById_found() {
        when(claimKpiRepository.findById(1L)).thenReturn(Optional.of(kpi));
        when(claimKpiMapper.toResponseDTO(kpi)).thenReturn(dto);

        assertNotNull(claimKpiService.getKpiById(1L));
    }

    @Test
    void getKpiById_notFound_throwsException() {
        when(claimKpiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> claimKpiService.getKpiById(99L));
    }

    @Test
    void getKpisByMetricName_invalidName_throwsException() {
        assertThrows(InvalidMetricException.class,
                () -> claimKpiService.getKpisByMetricName("WRONG"));
    }

    @Test
    void deleteKpi_success() {
        when(claimKpiRepository.findById(1L)).thenReturn(Optional.of(kpi));

        claimKpiService.deleteKpi(1L);

        verify(claimKpiRepository).deleteById(1L);
    }

    @Test
    void deleteKpi_notFound_throwsException() {
        when(claimKpiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> claimKpiService.deleteKpi(99L));
    }
}
