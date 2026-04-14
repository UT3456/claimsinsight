package com.demo.repositorytesting;

import com.demo.entities.AdjusterPerformance;
import com.demo.repositories.AdjusterPerformanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository Tests for AdjusterPerformanceRepository
 */
@DataJpaTest
public class AdjusterPerformanceRepositoryTests {

    @Autowired
    private AdjusterPerformanceRepository repository;

    private AdjusterPerformance perf1, perf2, perf3;

    @BeforeEach
    void setUp() {
        perf1 = new AdjusterPerformance();
        perf1.setAdjusterId(101L);
        perf1.setClaimsHandled(25);
        perf1.setTotalDaysTaken(100);
        perf1.setAvgTat(4.0);
        perf1.setSlaMetCount(22);
        perf1.setSlaBreachedCount(3);
        perf1.setDeniedClaimsCount(2);
        perf1.setErrorRate(8.0);
        perf1.setQualityScore(88.0);
        perf1.setPeriod("2026-Q1");

        perf2 = new AdjusterPerformance();
        perf2.setAdjusterId(102L);
        perf2.setClaimsHandled(10);
        perf2.setTotalDaysTaken(80);
        perf2.setAvgTat(8.0);
        perf2.setSlaMetCount(6);
        perf2.setSlaBreachedCount(4);
        perf2.setDeniedClaimsCount(3);
        perf2.setErrorRate(25.0);
        perf2.setQualityScore(62.5);
        perf2.setPeriod("2026-Q1");

        perf3 = new AdjusterPerformance();
        perf3.setAdjusterId(103L);
        perf3.setClaimsHandled(50);
        perf3.setTotalDaysTaken(150);
        perf3.setAvgTat(3.0);
        perf3.setSlaMetCount(48);
        perf3.setSlaBreachedCount(2);
        perf3.setDeniedClaimsCount(2);
        perf3.setErrorRate(3.0);
        perf3.setQualityScore(95.5);
        perf3.setPeriod("2026-Q2");

        repository.save(perf1);
        repository.save(perf2);
        repository.save(perf3);
    }

    // ── findByAdjusterIdAndPeriod ─────────────────────────────────────────

    @Test
    void testFindByAdjusterIdAndPeriod_positive() {
        Optional<AdjusterPerformance> result = repository.findByAdjusterIdAndPeriod(101L, "2026-Q1");
        assertTrue(result.isPresent());
        assertEquals(101L, result.get().getAdjusterId());
        assertEquals(25,   result.get().getClaimsHandled());
    }

    @Test
    void testFindByAdjusterIdAndPeriod_negative() {
        Optional<AdjusterPerformance> result = repository.findByAdjusterIdAndPeriod(999L, "2026-Q1");
        assertFalse(result.isPresent());
    }

    // ── findByPeriod ──────────────────────────────────────────────────────

    @Test
    void testFindByPeriod_positive() {
        List<AdjusterPerformance> result = repository.findByPeriod("2026-Q1");
        assertThat(result).hasSize(2);
    }

    @Test
    void testFindByPeriod_negative() {
        List<AdjusterPerformance> result = repository.findByPeriod("2099-Q9");
        assertThat(result).isEmpty();
    }

    // ── findByPeriodOrderByErrorRateAscSlaBreachedCountAsc (Top Performers) ──

    @Test
    void testFindTopPerformers_positive() {
        List<AdjusterPerformance> result = repository.findByPeriodOrderByErrorRateAscSlaBreachedCountAsc("2026-Q1");
        assertThat(result).hasSize(2);
        // perf1 errorRate(8) < perf2 errorRate(25) → perf1 first
        assertEquals(101L, result.get(0).getAdjusterId());
        assertEquals(102L, result.get(1).getAdjusterId());
    }

    @Test
    void testFindTopPerformers_negative() {
        List<AdjusterPerformance> result = repository.findByPeriodOrderByErrorRateAscSlaBreachedCountAsc("2099-Q9");
        assertThat(result).isEmpty();
    }

    // ── findByClaimsHandledLessThanAndPeriod ──────────

    @Test
    void testFindLowProductivity_positive() {
        List<AdjusterPerformance> result = repository.findByClaimsHandledLessThanAndPeriod(20, "2026-Q1");
        assertThat(result).hasSize(1);
        assertEquals(102L, result.get(0).getAdjusterId()); // claimsHandled=10
    }

    @Test
    void testFindLowProductivity_negative() {
        List<AdjusterPerformance> result = repository.findByClaimsHandledLessThanAndPeriod(5, "2026-Q1");
        assertThat(result).isEmpty();
    }

    // ── findByClaimsHandledGreaterThanAndPeriod (Overloaded) ─────────────

    @Test
    void testFindOverloaded_positive() {
        List<AdjusterPerformance> result = repository.findByClaimsHandledGreaterThanAndPeriod(40, "2026-Q2");
        assertThat(result).hasSize(1);
        assertEquals(103L, result.get(0).getAdjusterId()); // claimsHandled=50
    }

    @Test
    void testFindOverloaded_negative() {
        List<AdjusterPerformance> result = repository.findByClaimsHandledGreaterThanAndPeriod(100, "2026-Q1");
        assertThat(result).isEmpty();
    }

    // ── findByErrorRateGreaterThanAndPeriod (Training Required) ──────────

    @Test
    void testFindTrainingRequired_positive() {
        // qualityScore < 70 → errorRate > 20
        List<AdjusterPerformance> result = repository.findByErrorRateGreaterThanAndPeriod(20.0, "2026-Q1");
        assertThat(result).hasSize(1);
        assertEquals(102L, result.get(0).getAdjusterId()); // errorRate=25
    }

    @Test
    void testFindTrainingRequired_negative() {
        List<AdjusterPerformance> result = repository.findByErrorRateGreaterThanAndPeriod(90.0, "2026-Q1");
        assertThat(result).isEmpty();
    }

    // ── findByHighDenialRate ──────────────────────────────────────────────

    @Test
    void testFindHighDenialRate_positive() {
        List<AdjusterPerformance> result = repository.findByHighDenialRate("2026-Q1", 15.0);
        assertThat(result).hasSize(1);
        assertEquals(102L, result.get(0).getAdjusterId());
    }

    @Test
    void testFindHighDenialRate_negative() {
        List<AdjusterPerformance> result = repository.findByHighDenialRate("2026-Q1", 50.0);
        assertThat(result).isEmpty();
    }

    // ── findSlowPerformers ────────────────────────────────────────────────

    @Test
    void testFindSlowPerformers_positive() {
        // perf2: avgTat = 8.0 > 5.0 → slow performer
        List<AdjusterPerformance> result = repository.findSlowPerformers("2026-Q1", 5.0);
        assertThat(result).hasSize(1);
        assertEquals(102L, result.get(0).getAdjusterId());
    }

    @Test
    void testFindSlowPerformers_negative() {
        List<AdjusterPerformance> result = repository.findSlowPerformers("2026-Q1", 10.0);
        assertThat(result).isEmpty();
    }

    // ── findBelowSlaComplianceThreshold ──────────────────────────────────

    @Test
    void testFindBelowSlaCompliance_positive() {
        List<AdjusterPerformance> result = repository.findBelowSlaComplianceThreshold("2026-Q1");
        assertThat(result).hasSize(2);
        assertEquals(101L, result.get(0).getAdjusterId());
    }

    @Test
    void testFindBelowSlaCompliance_negative() {
        List<AdjusterPerformance> result = repository.findBelowSlaComplianceThreshold("2026-Q2");
        assertThat(result).isEmpty();
    }

    // ── findAverageClaimsHandledByPeriod ──────────────────────────────────

    @Test
    void testFindAverageClaimsHandledByPeriod_positive() {
        Double avg = repository.findAverageClaimsHandledByPeriod("2026-Q1");
        assertNotNull(avg);
        assertEquals((25.0 + 10.0) / 2, avg, 0.01);
    }

    // ── save & delete ─────────────────────────────────────────────────────

    @Test
    void testSave_positive() {
        AdjusterPerformance newPerf = new AdjusterPerformance();
        newPerf.setAdjusterId(104L);
        newPerf.setClaimsHandled(18);
        newPerf.setTotalDaysTaken(72);
        newPerf.setAvgTat(4.0);        // 72/18 = 4.0
        newPerf.setSlaMetCount(15);
        newPerf.setSlaBreachedCount(3);
        newPerf.setDeniedClaimsCount(2);
        newPerf.setErrorRate(10.0);
        newPerf.setQualityScore(85.0); // 100-(10*1.5) = 85.0
        newPerf.setPeriod("2026-Q1");
        AdjusterPerformance saved = repository.save(newPerf);
        assertNotNull(saved.getPerfId());
    }

    @Test
    void testDelete_positive() {
        repository.deleteById(perf1.getPerfId());
        assertFalse(repository.existsById(perf1.getPerfId()));
    }

    @AfterEach
    void tearDown() {
        perf1 = null;
        perf2 = null;
        perf3 = null;
    }
}