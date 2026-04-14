
package com.claims.service;

import com.claims.dto.request.AnalyticsReportRequest;
import com.claims.dto.response.AnalyticsReportResponse;
import com.claims.entity.AnalyticsReport;
import com.claims.enums.ReportScope;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.AnalyticsReportRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsReportServiceImpl implements AnalyticsReportService {

    private final AnalyticsReportRepository analyticsReportRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public AnalyticsReportServiceImpl(AnalyticsReportRepository analyticsReportRepository,
                                      ModelMapper modelMapper) {
        this.analyticsReportRepository = analyticsReportRepository;
        this.modelMapper = modelMapper;
    }

    // -------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------

    @Override
    public AnalyticsReportResponse createReport(AnalyticsReportRequest request) {
        AnalyticsReport report = modelMapper.map(request, AnalyticsReport.class);
        AnalyticsReport saved = analyticsReportRepository.save(report);
        return modelMapper.map(saved, AnalyticsReportResponse.class);
    }

    @Override
    public AnalyticsReportResponse getReportById(Long reportId) {
        AnalyticsReport report = findReportByIdOrThrow(reportId);
        return modelMapper.map(report, AnalyticsReportResponse.class);
    }

    @Override
    public List<AnalyticsReportResponse> getAllReports() {
        return analyticsReportRepository.findAll()
                .stream()
                .map(report -> modelMapper.map(report, AnalyticsReportResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public AnalyticsReportResponse updateReport(Long reportId, AnalyticsReportRequest request) {
        AnalyticsReport existing = findReportByIdOrThrow(reportId);
        modelMapper.map(request, existing);
        AnalyticsReport updated = analyticsReportRepository.save(existing);
        return modelMapper.map(updated, AnalyticsReportResponse.class);
    }

    @Override
    public void deleteReport(Long reportId) {
        AnalyticsReport existing = findReportByIdOrThrow(reportId);
        analyticsReportRepository.delete(existing);
    }

    // -------------------------------------------------------
    // Query Operations
    // -------------------------------------------------------

    @Override
    public List<AnalyticsReportResponse> getReportsByScope(ReportScope scope) {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScope(scope);
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found for scope: " + scope);
        }
        return reports.stream()
                .map(report -> modelMapper.map(report, AnalyticsReportResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsReportResponse> getReportsByScopeValue(String scopeValue) {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScopeValue(scopeValue);
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found for scopeValue: " + scopeValue);
        }
        return reports.stream()
                .map(report -> modelMapper.map(report, AnalyticsReportResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsReportResponse> getReportsByScopeAndScopeValue(ReportScope scope, String scopeValue) {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScopeAndScopeValue(scope, scopeValue);
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No reports found for scope: " + scope + " and scopeValue: " + scopeValue);
        }
        return reports.stream()
                .map(report -> modelMapper.map(report, AnalyticsReportResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsReportResponse> getReportsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedDateBetween(startDate, endDate);
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No reports found between " + startDate + " and " + endDate);
        }
        return reports.stream()
                .map(report -> modelMapper.map(report, AnalyticsReportResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsReportResponse> getReportsByGeneratedBy(String generatedBy) {
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedBy(generatedBy);
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found generated by: " + generatedBy);
        }
        return reports.stream()
                .map(report -> modelMapper.map(report, AnalyticsReportResponse.class))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // Analytics & Business Intelligence
    // -------------------------------------------------------

    @Override
    public AnalyticsReportResponse getLatestReport(ReportScope scope, String scopeValue) {
        List<AnalyticsReport> reports = analyticsReportRepository
                .findLatestByScopeAndScopeValue(scope, scopeValue);
        if (reports.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No reports found for scope: " + scope + " and scopeValue: " + scopeValue);
        }
        return modelMapper.map(reports.get(0), AnalyticsReportResponse.class);
    }

    @Override
    public Map<String, Long> getReportCountByScope() {
        List<Object[]> results = analyticsReportRepository.countByScope();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No reports found in the system");
        }
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Object[] row : results) {
            ReportScope scope = (ReportScope) row[0];
            Long count = (Long) row[1];
            countMap.put(scope.name(), count);
        }
        return countMap;
    }

    @Override
    public Map<String, Long> getReportCountByScopeValue() {
        List<AnalyticsReport> allReports = analyticsReportRepository.findAll();
        if (allReports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found in the system");
        }
        return allReports.stream()
                .collect(Collectors.groupingBy(AnalyticsReport::getScopeValue, Collectors.counting()));
    }

    @Override
    public Map<String, Long> getMonthlyReportGenerationTrend() {
        List<AnalyticsReport> allReports = analyticsReportRepository.findAll();
        if (allReports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found in the system");
        }
        return allReports.stream()
                .sorted((a, b) -> a.getGeneratedDate().compareTo(b.getGeneratedDate()))
                .collect(Collectors.groupingBy(
                        r -> r.getGeneratedDate().getYear() + "-"
                                + String.format("%02d", r.getGeneratedDate().getMonthValue()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Object> getMostActiveReportGenerator() {
        List<AnalyticsReport> allReports = analyticsReportRepository.findAll();
        if (allReports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found in the system");
        }
        Map<String, Long> countPerUser = allReports.stream()
                .filter(r -> r.getGeneratedBy() != null)
                .collect(Collectors.groupingBy(AnalyticsReport::getGeneratedBy, Collectors.counting()));

        String topUser = countPerUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new ResourceNotFoundException("Could not determine most active generator"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedBy", topUser);
        result.put("reportCount", countPerUser.get(topUser));
        return result;
    }

    @Override
    public Map<String, Object> getReportDashboardSummary() {
        List<AnalyticsReport> allReports = analyticsReportRepository.findAll();
        if (allReports.isEmpty()) {
            throw new ResourceNotFoundException("No reports found in the system");
        }

        long totalReports = allReports.size();

        Map<String, Long> perScope = allReports.stream()
                .collect(Collectors.groupingBy(r -> r.getScope().name(), Collectors.counting()));

        LocalDate latestDate = allReports.stream()
                .map(AnalyticsReport::getGeneratedDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        Map<String, Long> perUser = allReports.stream()
                .filter(r -> r.getGeneratedBy() != null)
                .collect(Collectors.groupingBy(AnalyticsReport::getGeneratedBy, Collectors.counting()));
        String topUser = perUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalReports", totalReports);
        summary.put("reportsByScope", perScope);
        summary.put("latestReportDate", latestDate);
        summary.put("mostActiveGenerator", topUser);
        return summary;
    }

    // -------------------------------------------------------
    // Private Helper
    // -------------------------------------------------------

    private AnalyticsReport findReportByIdOrThrow(Long reportId) {
        return analyticsReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("AnalyticsReport", "reportId", reportId));
    }
}

