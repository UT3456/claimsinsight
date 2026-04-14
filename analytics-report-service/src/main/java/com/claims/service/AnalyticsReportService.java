package com.claims.service;

import com.claims.dto.request.AnalyticsReportRequest;
import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.enums.ReportScope;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalyticsReportService {

    AnalyticsReportResponse createReport(AnalyticsReportRequest request);
    AnalyticsReportResponse getReportById(Long reportId);
    List<AnalyticsReportResponse> getAllReports();
    AnalyticsReportResponse updateReport(Long reportId, AnalyticsReportRequest request);
    void deleteReport(Long reportId);

    List<AnalyticsReportResponse> getReportsByScope(ReportScope scope);
    List<AnalyticsReportResponse> getReportsByScopeValue(String scopeValue);
    List<AnalyticsReportResponse> getReportsByScopeAndScopeValue(ReportScope scope, String scopeValue);
    List<AnalyticsReportResponse> getReportsByDateRange(LocalDate startDate, LocalDate endDate);
    List<AnalyticsReportResponse> getReportsByGeneratedBy(String generatedBy);

    AnalyticsReportResponse getLatestReport(ReportScope scope, String scopeValue);
    Map<String, Long> getReportCountByScope();
    Map<String, Long> getReportCountByScopeValue();
    Map<String, Long> getMonthlyReportGenerationTrend();
    Map<String, Object> getMostActiveReportGenerator();
    Map<String, Object> getReportDashboardSummary();
}


