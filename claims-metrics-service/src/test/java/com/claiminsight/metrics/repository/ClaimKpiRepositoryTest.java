package com.claiminsight.metrics.repository;

import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.MetricName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClaimKpiRepositoryTest {

    @Autowired ClaimKpiRepository claimKpiRepository;

    private ClaimKPI buildKpi(String claimId, MetricName name, double value, LocalDate date) {
        ClaimKPI kpi = new ClaimKPI();
        kpi.setClaimId(claimId);
        kpi.setMetricName(name);
        kpi.setMetricValue(BigDecimal.valueOf(value));
        kpi.setMetricDate(date);
        return kpi;
    }

    @Test
    void save_assignsId() {
        ClaimKPI saved = claimKpiRepository.save(
                buildKpi("CLM-001", MetricName.TAT, 5.0, LocalDate.now()));

        assertNotNull(saved.getKpiId());
    }

    @Test
    void findByClaimId_returnsMatching() {
        claimKpiRepository.save(buildKpi("CLM-001", MetricName.TAT,        5.0, LocalDate.now()));
        claimKpiRepository.save(buildKpi("CLM-001", MetricName.SEVERITY,   3.5, LocalDate.now()));
        claimKpiRepository.save(buildKpi("CLM-002", MetricName.CYCLE_TIME, 8.0, LocalDate.now()));

        List<ClaimKPI> result = claimKpiRepository.findByClaimId("CLM-001");

        assertEquals(2, result.size());
    }

    @Test
    void findByMetricName_returnsMatching() {
        claimKpiRepository.save(buildKpi("CLM-001", MetricName.TAT, 5.0, LocalDate.now()));
        claimKpiRepository.save(buildKpi("CLM-002", MetricName.TAT, 6.0, LocalDate.now()));
        claimKpiRepository.save(buildKpi("CLM-003", MetricName.SEVERITY, 3.0, LocalDate.now()));

        assertEquals(2, claimKpiRepository.findByMetricName(MetricName.TAT).size());
    }

    @Test
    void findByMetricDateBetween_returnsInRange() {
        LocalDate jan = LocalDate.of(2026, 1, 15);
        LocalDate feb = LocalDate.of(2026, 2, 15);
        LocalDate mar = LocalDate.of(2026, 3, 15);

        claimKpiRepository.save(buildKpi("CLM-001", MetricName.TAT, 5.0, jan));
        claimKpiRepository.save(buildKpi("CLM-002", MetricName.TAT, 6.0, feb));
        claimKpiRepository.save(buildKpi("CLM-003", MetricName.TAT, 7.0, mar));

        List<ClaimKPI> result = claimKpiRepository.findByMetricDateBetween(jan, feb);

        assertEquals(2, result.size());
    }

    @Test
    void deleteById_removesRecord() {
        ClaimKPI saved = claimKpiRepository.save(
                buildKpi("CLM-001", MetricName.TAT, 5.0, LocalDate.now()));

        claimKpiRepository.deleteById(saved.getKpiId());

        assertFalse(claimKpiRepository.findById(saved.getKpiId()).isPresent());
    }
}
