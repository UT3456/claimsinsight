package com.demo.repositorytesting;
import com.demo.entities.SLAViolation;
import com.demo.repositories.SLAViolationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository Tests for SLAViolationRepository.
 *
 * <p>{@code claimId} is a soft reference (plain {@code Long}) — the {@code claim}
 * table is owned by claims-metrics-service, so we just store the id with no FK.</p>
 */
@DataJpaTest
public class SLAViolationRepositoryTests {

    private static final Long CLAIM_ID = 500L;

    @Autowired
    private SLAViolationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private SLAViolation v1, v2, v3, v4;

    @BeforeEach
    void setUp() {
        // v1 — LOW severity (daysOverdue=2)
        v1 = new SLAViolation();
        v1.setAdjusterId(101L);
        v1.setViolationType("PAYMENT_DELAY");
        v1.setSlaTargetDays(10);
        v1.setActualDays(12);         // daysOverdue = 2
        v1.setViolationDate(new Date());
        v1.setClaimId(CLAIM_ID);

        // v2 — MEDIUM severity (daysOverdue=5)
        v2 = new SLAViolation();
        v2.setAdjusterId(101L);
        v2.setViolationType("REVIEW_DELAY");
        v2.setSlaTargetDays(5);
        v2.setActualDays(10);
        v2.setViolationDate(new Date());
        v2.setClaimId(CLAIM_ID);

        v3 = new SLAViolation();
        v3.setAdjusterId(102L);
        v3.setViolationType("DECISION_DELAY");
        v3.setSlaTargetDays(7);
        v3.setViolationDate(new Date());
        v3.setClaimId(CLAIM_ID);

        v4 = new SLAViolation();
        v4.setAdjusterId(102L);
        v4.setViolationType("NOTIFICATION_DELAY");
        v4.setSlaTargetDays(3);
        v4.setActualDays(20);
        v4.setViolationDate(new Date());
        v4.setClaimId(CLAIM_ID);

        repository.save(v1);
        repository.save(v2);
        repository.save(v3);
        repository.save(v4);
    }

    // ── findByAdjusterId ──────────────────────────────────────────────────

    @Test
    void testFindByAdjusterId_positive() {
        List<SLAViolation> result = repository.findByAdjusterId(101L);
        assertThat(result).hasSize(2);
    }

    @Test
    void testFindByAdjusterId_negative() {
        List<SLAViolation> result = repository.findByAdjusterId(999L);
        assertThat(result).isEmpty();
    }

    // ── findByAdjusterIdAndViolationDateBetween ───────────────────────────

    @Test
    void testFindByAdjusterIdAndDateRange_positive() {
        Date start = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
        Date end   = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
        List<SLAViolation> result = repository.findByAdjusterIdAndViolationDateBetween(101L, start, end);
        assertThat(result).hasSize(2);
    }

    @Test
    void testFindByAdjusterIdAndDateRange_negative() {
        Date start = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 10);
        Date end   = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 5);
        List<SLAViolation> result = repository.findByAdjusterIdAndViolationDateBetween(101L, start, end);
        assertThat(result).isEmpty();
    }

    // ── findByClaimId ─────────────────────────────────────────────────────

    @Test
    void testFindByClaimId_positive() {
        List<SLAViolation> result = repository.findByClaimId(CLAIM_ID);
        assertThat(result).hasSize(4);
    }

    @Test
    void testFindByClaimId_negative() {
        List<SLAViolation> result = repository.findByClaimId(999L);
        assertThat(result).isEmpty();
    }

    // ── findByViolationType ───────────────────────────────────────────────

    @Test
    void testFindByViolationType_positive() {
        List<SLAViolation> result = repository.findByViolationType("PAYMENT_DELAY");
        assertThat(result).hasSize(1);
        assertEquals("PAYMENT_DELAY", result.get(0).getViolationType());
    }

    @Test
    void testFindByViolationType_negative() {
        List<SLAViolation> result = repository.findByViolationType("UNKNOWN");
        assertThat(result).isEmpty();
    }

    // ── findByDaysOverdueGreaterThan ──────────────────────────────────────

    @Test
    void testFindByDaysOverdueGreaterThan_positive() {
        List<SLAViolation> result = repository.findByDaysOverdueGreaterThan(5);
        assertThat(result).hasSize(1);
    }

    @Test
    void testFindByDaysOverdueGreaterThan_negative() {
        List<SLAViolation> result = repository.findByDaysOverdueGreaterThan(20);
        assertThat(result).isEmpty();
    }

    // ── findBySeverityRange ───────────────────────────────────────────────

    @Test
    void testFindBySeverityRange_LOW() {
        // daysOverdue 1-2: only v1(daysOverdue=2)
        List<SLAViolation> result = repository.findBySeverityRange(1, 2);
        assertThat(result).hasSize(1);
    }

    @Test
    void testFindBySeverityRange_MEDIUM() {
        // daysOverdue 3-5: only v2(daysOverdue=5)
        List<SLAViolation> result = repository.findBySeverityRange(3, 5);
        assertThat(result).hasSize(1);
    }

    // ── findEscalationCandidatesByAdjuster ────────────────────────────────

    @Test
    void testFindEscalationCandidates_positive() {
        // adjuster 102 has v3(daysOverdue=8) and v4(daysOverdue=17) — both > 5
        List<SLAViolation> result = repository.findEscalationCandidatesByAdjuster(102L);
        assertThat(result).hasSize(1);
    }

    @Test
    void testFindEscalationCandidates_noneForAdjuster101() {
        // adjuster 101 has max daysOverdue=5 — NOT > 5
        List<SLAViolation> result =
                repository.findEscalationCandidatesByAdjuster(101L);
        assertThat(result).isEmpty();
    }

    // ── countViolationsByAdjusterAndPeriod ────────────────────────────────

    @Test
    void testCountViolations_positive() {
        Date start = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
        Date end   = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
        Long count = repository.countViolationsByAdjusterAndPeriod(101L, start, end);
        assertEquals(2L, count);
    }

    @Test
    void testCountViolations_reviewTrigger() {
        // adjuster 102 has 2 violations in window — test that count returns correctly
        Date start = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
        Date end   = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
        Long count = repository.countViolationsByAdjusterAndPeriod(102L, start, end);
        assertEquals(2L, count);
    }

    // ── findTotalDaysOverdueByClaim ───────────────────────────────────────

    @Test
    void testFindTotalDaysOverdueByClaim_positive() {
        Integer total = repository.findTotalDaysOverdueByClaim(CLAIM_ID);
        assertEquals(17, total);
    }

    @Test
    void testFindTotalDaysOverdueByClaim_noViolations_returnsZero() {
        Integer total = repository.findTotalDaysOverdueByClaim(999L);
        assertEquals(0, total);
    }

    // ── countAllViolationsByAdjuster ──────────────────────────────────────

    @Test
    void testCountAllViolationsByAdjuster_positive() {
        Long count = repository.countAllViolationsByAdjuster(101L);
        assertEquals(2L, count);
    }

    @Test
    void testCountAllViolationsByAdjuster_zero() {
        Long count = repository.countAllViolationsByAdjuster(999L);
        assertEquals(0L, count);
    }

    // ── findByViolationDateBetween ────────────────────────────────────────

    @Test
    void testFindByViolationDateBetween_positive() {
        Date start = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
        Date end   = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
        List<SLAViolation> result = repository.findByViolationDateBetween(start, end);
        assertThat(result).hasSize(4);
    }

    @Test
    void testFindByViolationDateBetween_negative() {
        Date start = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 10);
        Date end   = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 5);
        List<SLAViolation> result = repository.findByViolationDateBetween(start, end);
        assertThat(result).isEmpty();
    }

    // ── save & delete ─────────────────────────────────────────────────────

    @Test
    void testSave_positive() {
        SLAViolation newV = new SLAViolation();
        newV.setAdjusterId(103L);
        newV.setViolationType("REVIEW_DELAY");
        newV.setSlaTargetDays(5);
        newV.setActualDays(9);
        newV.setViolationDate(new Date());
        newV.setClaimId(CLAIM_ID);
        SLAViolation saved = repository.save(newV);
        assertNotNull(saved.getViolationId());
    }

    @Test
    void testDelete_positive() {
        repository.deleteById(v1.getViolationId());
        assertFalse(repository.existsById(v1.getViolationId()));
    }

    @AfterEach
    void tearDown() {
        v1 = null; v2 = null; v3 = null; v4 = null;
    }
}
