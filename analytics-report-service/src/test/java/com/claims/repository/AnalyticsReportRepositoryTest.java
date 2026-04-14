package com.claims.repository;

import com.claims.entity.AnalyticsReport;
import com.claims.enums.ReportScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AnalyticsReportRepositoryTest {

    @Autowired
    private AnalyticsReportRepository analyticsReportRepository;

    @BeforeEach
    void setUp() {
        analyticsReportRepository.deleteAll();

        analyticsReportRepository.save(new AnalyticsReport(null, ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 1, 10), "admin", "{\"tat\":5.2}"));
        analyticsReportRepository.save(new AnalyticsReport(null, ReportScope.REGION, "South Region",
                "ClaimVolume", LocalDate.of(2024, 2, 15), "admin", "{\"volume\":120}"));
        analyticsReportRepository.save(new AnalyticsReport(null, ReportScope.PRODUCT, "Auto",
                "LossRatio, DenialRate", LocalDate.of(2024, 3, 1), "john.doe", "{\"lossRatio\":0.65}"));
        analyticsReportRepository.save(new AnalyticsReport(null, ReportScope.PRODUCT, "Health",
                "ClaimVolume, TAT", LocalDate.of(2024, 3, 20), "john.doe", "{\"volume\":200}"));
        analyticsReportRepository.save(new AnalyticsReport(null, ReportScope.PERIOD, "Q1-2024",
                "TAT, LossRatio, ClaimVolume", LocalDate.of(2024, 4, 5), "admin", "{\"summary\":\"Q1\"}"));
        analyticsReportRepository.save(new AnalyticsReport(null, ReportScope.REGION, "North Region",
                "TAT, LossRatio", LocalDate.of(2024, 5, 10), "admin", "{\"tat\":4.8}"));
    }

    // -------------------------------------------------------
    // Save & Find by ID
    // -------------------------------------------------------

    @Test
    void testSaveReport_success() {
        AnalyticsReport report = new AnalyticsReport(null, ReportScope.CLAIM_TYPE, "Fire",
                "LossRatio", LocalDate.now(), "jane.doe", "{}");
        AnalyticsReport saved = analyticsReportRepository.save(report);

        assertThat(saved.getReportId()).isNotNull();
        assertThat(saved.getScope()).isEqualTo(ReportScope.CLAIM_TYPE);
        assertThat(saved.getScopeValue()).isEqualTo("Fire");
    }

    @Test
    void testFindById_success() {
        AnalyticsReport report = analyticsReportRepository.save(new AnalyticsReport(
                null, ReportScope.PERIOD, "Q2-2024", "TAT", LocalDate.now(), "admin", "{}"));
        assertThat(analyticsReportRepository.findById(report.getReportId())).isPresent();
    }

    @Test
    void testFindById_notFound() {
        assertThat(analyticsReportRepository.findById(999L)).isEmpty();
    }

    // -------------------------------------------------------
    // Find by Scope
    // -------------------------------------------------------

    @Test
    void testFindByScope_region_returnsThreeRecords() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScope(ReportScope.REGION);
        assertThat(reports).hasSize(3);
        assertThat(reports).allMatch(r -> r.getScope() == ReportScope.REGION);
    }

    @Test
    void testFindByScope_claimType_returnsEmpty() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScope(ReportScope.CLAIM_TYPE);
        assertThat(reports).isEmpty();
    }

    // -------------------------------------------------------
    // Find by ScopeValue
    // -------------------------------------------------------

    @Test
    void testFindByScopeValue_northRegion_returnsTwoRecords() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScopeValue("North Region");
        assertThat(reports).hasSize(2);
    }

    @Test
    void testFindByScopeValue_noMatch_returnsEmpty() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByScopeValue("East Region");
        assertThat(reports).isEmpty();
    }

    // -------------------------------------------------------
    // Find by Scope and ScopeValue
    // -------------------------------------------------------

    @Test
    void testFindByScopeAndScopeValue_success() {
        List<AnalyticsReport> reports = analyticsReportRepository
                .findByScopeAndScopeValue(ReportScope.PRODUCT, "Auto");
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getGeneratedBy()).isEqualTo("john.doe");
    }

    @Test
    void testFindByScopeAndScopeValue_noMatch_returnsEmpty() {
        List<AnalyticsReport> reports = analyticsReportRepository
                .findByScopeAndScopeValue(ReportScope.REGION, "Auto");
        assertThat(reports).isEmpty();
    }

    // -------------------------------------------------------
    // Find by Date Range
    // -------------------------------------------------------

    @Test
    void testFindByGeneratedDateBetween_returnsCorrectRecords() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedDateBetween(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 28));
        assertThat(reports).hasSize(2);
    }

    @Test
    void testFindByGeneratedDateBetween_noMatch_returnsEmpty() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedDateBetween(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        assertThat(reports).isEmpty();
    }

    // -------------------------------------------------------
    // Find by GeneratedBy
    // -------------------------------------------------------

    @Test
    void testFindByGeneratedBy_admin_returnsFourRecords() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedBy("admin");
        assertThat(reports).hasSize(4);
    }

    @Test
    void testFindByGeneratedBy_johnDoe_returnsTwoRecords() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedBy("john.doe");
        assertThat(reports).hasSize(2);
    }

    @Test
    void testFindByGeneratedBy_noMatch_returnsEmpty() {
        List<AnalyticsReport> reports = analyticsReportRepository.findByGeneratedBy("unknown.user");
        assertThat(reports).isEmpty();
    }

    // -------------------------------------------------------
    // Find Latest by Scope and ScopeValue
    // -------------------------------------------------------

    @Test
    void testFindLatestByScopeAndScopeValue_returnsLatestFirst() {
        List<AnalyticsReport> reports = analyticsReportRepository
                .findLatestByScopeAndScopeValue(ReportScope.REGION, "North Region");

        assertThat(reports).isNotEmpty();
        // First result should be the most recently generated one (May 2024)
        assertThat(reports.get(0).getGeneratedDate()).isEqualTo(LocalDate.of(2024, 5, 10));
    }

    @Test
    void testFindLatestByScopeAndScopeValue_noMatch_returnsEmpty() {
        List<AnalyticsReport> reports = analyticsReportRepository
                .findLatestByScopeAndScopeValue(ReportScope.REGION, "West Region");
        assertThat(reports).isEmpty();
    }

    // -------------------------------------------------------
    // Count by Scope
    // -------------------------------------------------------

    @Test
    void testCountByScope_returnsResults() {
        List<Object[]> results = analyticsReportRepository.countByScope();
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(row -> {
            assertThat(row[0]).isInstanceOf(ReportScope.class);
            assertThat(row[1]).isInstanceOf(Long.class);
        });
    }

    @Test
    void testCountByScope_regionCountIsThree() {
        List<Object[]> results = analyticsReportRepository.countByScope();
        results.stream()
                .filter(row -> row[0] == ReportScope.REGION)
                .findFirst()
                .ifPresent(row -> assertThat((Long) row[1]).isEqualTo(3L));
    }

    // -------------------------------------------------------
    // Update & Delete
    // -------------------------------------------------------

    @Test
    void testUpdateReport_success() {
        AnalyticsReport report = analyticsReportRepository.findByScope(ReportScope.PERIOD).get(0);
        report.setMetrics("TAT, LossRatio, ClaimVolume, DenialRate");
        AnalyticsReport updated = analyticsReportRepository.save(report);

        assertThat(updated.getMetrics()).contains("DenialRate");
    }

    @Test
    void testDeleteReport_success() {
        AnalyticsReport report = analyticsReportRepository.findByScope(ReportScope.PERIOD).get(0);
        Long id = report.getReportId();
        analyticsReportRepository.delete(report);

        assertThat(analyticsReportRepository.findById(id)).isEmpty();
    }
}
