







package com.claims.service;

import com.claims.dto.request.AnalyticsReportRequest;
import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.entity.AnalyticsReport;
import com.claims.enums.ReportScope;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.AnalyticsReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsReportServiceTest {

    @Mock
    private AnalyticsReportRepository analyticsReportRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AnalyticsReportServiceImpl analyticsReportService;

    private AnalyticsReport report;
    private AnalyticsReportRequest request;
    private AnalyticsReportResponse response;

    @BeforeEach
    void setUp() {
        report = new AnalyticsReport(1L, ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 1, 10), "admin", "{\"tat\":5.2}");

        request = new AnalyticsReportRequest(ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 1, 10), "admin", "{\"tat\":5.2}");

        response = new AnalyticsReportResponse(1L, ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 1, 10), "admin", "{\"tat\":5.2}");
    }

    // -------------------------------------------------------
    // CRUD
    // -------------------------------------------------------

    @Test
    void testCreateReport_success() {
        when(modelMapper.map(request, AnalyticsReport.class)).thenReturn(report);
        when(analyticsReportRepository.save(report)).thenReturn(report);
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        AnalyticsReportResponse result = analyticsReportService.createReport(request);

        assertThat(result.getReportId()).isEqualTo(1L);
        assertThat(result.getScope()).isEqualTo(ReportScope.REGION);
        assertThat(result.getScopeValue()).isEqualTo("North Region");
        verify(analyticsReportRepository).save(report);
    }

    @Test
    void testGetReportById_success() {
        when(analyticsReportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        AnalyticsReportResponse result = analyticsReportService.getReportById(1L);

        assertThat(result.getReportId()).isEqualTo(1L);
    }

    @Test
    void testGetReportById_notFound_throwsException() {
        when(analyticsReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsReportService.getReportById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetAllReports_success() {
        when(analyticsReportRepository.findAll()).thenReturn(List.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        List<AnalyticsReportResponse> result = analyticsReportService.getAllReports();

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetAllReports_emptyList() {
        when(analyticsReportRepository.findAll()).thenReturn(List.of());

        List<AnalyticsReportResponse> result = analyticsReportService.getAllReports();

        assertThat(result).isEmpty();
    }



    @Test
    void testUpdateReport_notFound_throwsException() {
        when(analyticsReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsReportService.updateReport(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testDeleteReport_success() {
        when(analyticsReportRepository.findById(1L)).thenReturn(Optional.of(report));

        analyticsReportService.deleteReport(1L);

        verify(analyticsReportRepository).delete(report);
    }

    @Test
    void testDeleteReport_notFound_throwsException() {
        when(analyticsReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsReportService.deleteReport(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------
    // Query Operations
    // -------------------------------------------------------

    @Test
    void testGetReportsByScope_success() {
        when(analyticsReportRepository.findByScope(ReportScope.REGION)).thenReturn(List.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        List<AnalyticsReportResponse> result = analyticsReportService.getReportsByScope(ReportScope.REGION);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScope()).isEqualTo(ReportScope.REGION);
    }

    @Test
    void testGetReportsByScope_notFound_throwsException() {
        when(analyticsReportRepository.findByScope(ReportScope.PRODUCT)).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getReportsByScope(ReportScope.PRODUCT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReportsByScopeValue_success() {
        when(analyticsReportRepository.findByScopeValue("North Region")).thenReturn(List.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        List<AnalyticsReportResponse> result = analyticsReportService.getReportsByScopeValue("North Region");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetReportsByScopeValue_notFound_throwsException() {
        when(analyticsReportRepository.findByScopeValue("Unknown")).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getReportsByScopeValue("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReportsByScopeAndScopeValue_success() {
        when(analyticsReportRepository.findByScopeAndScopeValue(ReportScope.REGION, "North Region"))
                .thenReturn(List.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        List<AnalyticsReportResponse> result = analyticsReportService
                .getReportsByScopeAndScopeValue(ReportScope.REGION, "North Region");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetReportsByDateRange_invalidDates_throwsException() {
        assertThatThrownBy(() -> analyticsReportService.getReportsByDateRange(
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void testGetReportsByDateRange_notFound_throwsException() {
        when(analyticsReportRepository.findByGeneratedDateBetween(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getReportsByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReportsByGeneratedBy_success() {
        when(analyticsReportRepository.findByGeneratedBy("admin")).thenReturn(List.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        List<AnalyticsReportResponse> result = analyticsReportService.getReportsByGeneratedBy("admin");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetReportsByGeneratedBy_notFound_throwsException() {
        when(analyticsReportRepository.findByGeneratedBy("unknown")).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getReportsByGeneratedBy("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------
    // Analytics
    // -------------------------------------------------------

    @Test
    void testGetLatestReport_success() {
        when(analyticsReportRepository.findLatestByScopeAndScopeValue(ReportScope.REGION, "North Region"))
                .thenReturn(List.of(report));
        when(modelMapper.map(report, AnalyticsReportResponse.class)).thenReturn(response);

        AnalyticsReportResponse result = analyticsReportService
                .getLatestReport(ReportScope.REGION, "North Region");

        assertThat(result.getScopeValue()).isEqualTo("North Region");
    }

    @Test
    void testGetLatestReport_notFound_throwsException() {
        when(analyticsReportRepository.findLatestByScopeAndScopeValue(ReportScope.REGION, "Unknown"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getLatestReport(ReportScope.REGION, "Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReportCountByScopeValue_success() {
        AnalyticsReport report2 = new AnalyticsReport(2L, ReportScope.REGION, "North Region",
                "TAT", LocalDate.of(2024, 2, 1), "admin", "{}");

        when(analyticsReportRepository.findAll()).thenReturn(List.of(report, report2));

        Map<String, Long> result = analyticsReportService.getReportCountByScopeValue();

        assertThat(result.get("North Region")).isEqualTo(2L);
    }

    @Test
    void testGetReportCountByScopeValue_noReports_throwsException() {
        when(analyticsReportRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getReportCountByScopeValue())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetMonthlyReportGenerationTrend_success() {
        AnalyticsReport report2 = new AnalyticsReport(2L, ReportScope.PRODUCT, "Auto",
                "LossRatio", LocalDate.of(2024, 2, 5), "admin", "{}");

        when(analyticsReportRepository.findAll()).thenReturn(List.of(report, report2));

        Map<String, Long> result = analyticsReportService.getMonthlyReportGenerationTrend();

        assertThat(result).containsKey("2024-01");
        assertThat(result).containsKey("2024-02");
    }

    @Test
    void testGetMostActiveReportGenerator_success() {
        AnalyticsReport report2 = new AnalyticsReport(2L, ReportScope.PRODUCT, "Auto",
                "LossRatio", LocalDate.of(2024, 2, 1), "admin", "{}");

        when(analyticsReportRepository.findAll()).thenReturn(List.of(report, report2));

        Map<String, Object> result = analyticsReportService.getMostActiveReportGenerator();

        assertThat(result.get("generatedBy")).isEqualTo("admin");
        assertThat(result.get("reportCount")).isEqualTo(2L);
    }

    @Test
    void testGetMostActiveReportGenerator_noReports_throwsException() {
        when(analyticsReportRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getMostActiveReportGenerator())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReportDashboardSummary_success() {
        when(analyticsReportRepository.findAll()).thenReturn(List.of(report));

        Map<String, Object> result = analyticsReportService.getReportDashboardSummary();

        assertThat(result.get("totalReports")).isEqualTo(1L);
        assertThat(result.get("mostActiveGenerator")).isEqualTo("admin");
        assertThat(result.get("latestReportDate")).isEqualTo(LocalDate.of(2024, 1, 10));
    }

    @Test
    void testGetReportDashboardSummary_noReports_throwsException() {
        when(analyticsReportRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsReportService.getReportDashboardSummary())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}



