package com.claiminsight.metrics.service;

import com.claiminsight.metrics.dto.ClaimKpiResponseDTO;
import com.claiminsight.metrics.dto.KpiSummaryDTO;
import com.claiminsight.metrics.exception.ResourceNotFoundException;
import com.claiminsight.metrics.mapper.ClaimKpiMapper;
import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.ClaimRawProjection;
import com.claiminsight.metrics.model.MetricName;
import com.claiminsight.metrics.repository.ClaimKpiRepository;
import com.claiminsight.metrics.repository.ClaimRawProjectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiCalculationServiceTest {

    @Mock ClaimRawProjectionRepository claimRawProjectionRepository;
    @Mock ClaimKpiRepository claimKpiRepository;
    @Mock ClaimKpiMapper claimKpiMapper;

    @InjectMocks KpiCalculationService kpiCalculationService;

    @BeforeEach
    void setUp() {
        kpiCalculationService = new KpiCalculationService(
                claimRawProjectionRepository,
                claimKpiRepository,
                claimKpiMapper,
                new ObjectMapper()
        );

        lenient().when(claimKpiRepository.save(any(ClaimKPI.class))).thenAnswer(invocation -> {
            ClaimKPI saved = invocation.getArgument(0);
            saved.setKpiId((long) (saved.getMetricName().ordinal() + 1));
            return saved;
        });

        lenient().when(claimKpiMapper.toResponseDTO(any(ClaimKPI.class))).thenAnswer(invocation -> {
            ClaimKPI saved = invocation.getArgument(0);
            ClaimKpiResponseDTO response = new ClaimKpiResponseDTO();
            response.setKpiId(saved.getKpiId());
            response.setClaimId(saved.getClaimId());
            response.setMetricName(saved.getMetricName().name());
            response.setMetricValue(saved.getMetricValue());
            response.setMetricDate(saved.getMetricDate());
            return response;
        });
    }

    @Test
    void calculateAndSave_savesAllMetricsWithExpectedValues() {
        String claimId = "CLM-100";
        ClaimRawProjection first = rawRecord(
                1L,
                claimId,
                "{\"incidentDate\":\"2026-03-10\",\"claimAmount\":25000,\"premium\":50000}",
                LocalDateTime.of(2026, 3, 12, 9, 0)
        );
        ClaimRawProjection second = rawRecord(
                2L,
                claimId,
                "{\"incidentDate\":\"2026-03-10\",\"claimAmount\":25000,\"premium\":50000}",
                LocalDateTime.of(2026, 3, 15, 10, 0)
        );

        when(claimRawProjectionRepository.findByClaimId(claimId)).thenReturn(List.of(first, second));

        KpiSummaryDTO result = kpiCalculationService.calculateAndSave(claimId);

        assertEquals(claimId, result.getClaimId());
        assertEquals(new BigDecimal("10"), result.getTat());
        assertEquals(new BigDecimal("3"), result.getCycleTime());
        assertEquals(new BigDecimal("2.50"), result.getSeverity());
        assertEquals(new BigDecimal("2"), result.getFrequency());
        assertEquals(new BigDecimal("0.5000"), result.getLossRatio());
        assertEquals(LocalDate.of(2026, 3, 20), result.getCalculatedDate());
        assertEquals(5, result.getSavedKpis().size());

        ArgumentCaptor<ClaimKPI> captor = ArgumentCaptor.forClass(ClaimKPI.class);
        verify(claimKpiRepository, org.mockito.Mockito.times(5)).save(captor.capture());
        List<ClaimKPI> saved = captor.getAllValues();
        assertEquals(List.of(
                MetricName.TAT,
                MetricName.CYCLE_TIME,
                MetricName.SEVERITY,
                MetricName.FREQUENCY,
                MetricName.LOSS_RATIO
        ), saved.stream().map(ClaimKPI::getMetricName).toList());
    }

    @Test
    void calculateAndSave_usesFallbacksForMissingFieldsAndInvalidJson() {
        String claimId = "CLM-200";
        ClaimRawProjection record = rawRecord(
                3L,
                claimId,
                "{bad json",
                LocalDateTime.of(2026, 3, 20, 12, 0)
        );

        when(claimRawProjectionRepository.findByClaimId(claimId)).thenReturn(List.of(record));

        KpiSummaryDTO result = kpiCalculationService.calculateAndSave(claimId);

        assertEquals(new BigDecimal("1"), result.getTat());
        assertEquals(new BigDecimal("1"), result.getCycleTime());
        assertEquals(new BigDecimal("1.00"), result.getSeverity());
        assertEquals(new BigDecimal("1"), result.getFrequency());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getLossRatio()));
    }

    @Test
    void calculateAndSave_usesAdmissionDateAndCapsSeverityWhenClaimAmountIsHigh() {
        String claimId = "CLM-300";
        ClaimRawProjection record = rawRecord(
                4L,
                claimId,
                "{\"admissionDate\":\"2026-03-18\",\"amount\":200000,\"premium\":10000}",
                LocalDateTime.of(2026, 3, 20, 8, 30)
        );

        when(claimRawProjectionRepository.findByClaimId(claimId)).thenReturn(List.of(record));

        KpiSummaryDTO result = kpiCalculationService.calculateAndSave(claimId);

        assertEquals(new BigDecimal("2"), result.getTat());
        assertEquals(new BigDecimal("10.00"), result.getSeverity());
        assertEquals(new BigDecimal("20.0000"), result.getLossRatio());
    }

    @Test
    void calculateAndSave_returnsZeroLossRatioWhenPremiumIsMissing() {
        String claimId = "CLM-400";
        ClaimRawProjection record = rawRecord(
                5L,
                claimId,
                "{\"incidentDate\":\"2026-03-19\",\"totalClaim\":15000}",
                LocalDateTime.of(2026, 3, 20, 8, 30)
        );

        when(claimRawProjectionRepository.findByClaimId(claimId)).thenReturn(List.of(record));

        KpiSummaryDTO result = kpiCalculationService.calculateAndSave(claimId);

        assertEquals(new BigDecimal("1.50"), result.getSeverity());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getLossRatio()));
        assertTrue(result.getSavedKpis().stream()
                .anyMatch(kpi -> "LOSS_RATIO".equals(kpi.getMetricName()) && BigDecimal.ZERO.compareTo(kpi.getMetricValue()) == 0));
    }

    @Test
    void calculateAndSave_noRawRecords_throwsException() {
        when(claimRawProjectionRepository.findByClaimId("missing")).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> kpiCalculationService.calculateAndSave("missing"));
    }

    private ClaimRawProjection rawRecord(Long id, String claimId, String payloadJson, LocalDateTime ingestedDate) {
        ClaimRawProjection record = new ClaimRawProjection();
        record.setRawId(id);
        record.setClaimId(claimId);
        record.setPayloadJson(payloadJson);
        record.setIngestedDate(ingestedDate);
        return record;
    }
}
