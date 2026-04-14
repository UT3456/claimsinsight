package com.claim360.fraudrisk.repository;

import com.claim360.fraudrisk.entity.RiskScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RiskScoreRepositoryTest {

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    private RiskScore score1;
    private RiskScore score2;
    private RiskScore score3;

    @BeforeEach
    void setUp() {
        riskScoreRepository.deleteAll();

        score1 = new RiskScore();
        score1.setClaimId("CLM-001");
        score1.setScoreValue(85.5);
        score1.setComputedDate(LocalDate.of(2024, 1, 10));

        score2 = new RiskScore();
        score2.setClaimId("CLM-001");
        score2.setScoreValue(90.0);
        score2.setComputedDate(LocalDate.of(2024, 3, 20));

        score3 = new RiskScore();
        score3.setClaimId("CLM-002");
        score3.setScoreValue(45.0);
        score3.setComputedDate(LocalDate.of(2024, 2, 15));

        riskScoreRepository.save(score1);
        riskScoreRepository.save(score2);
        riskScoreRepository.save(score3);
    }

    // ── Save & Find ──────────────────────────────────────────────────────────

    @Test
    void save_ShouldPersistRiskScore() {
        List<RiskScore> all = riskScoreRepository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.get(0).getScoreId()).isNotNull();
    }

    @Test
    void findAll_ShouldReturnAllScores() {
        List<RiskScore> all = riskScoreRepository.findAll();
        assertThat(all).hasSize(3);
    }

    @Test
    void findById_WhenExists_ShouldReturnScore() {
        RiskScore saved = riskScoreRepository.findByClaimId("CLM-002").get(0);
        Optional<RiskScore> found = riskScoreRepository.findById(saved.getScoreId());
        assertThat(found).isPresent();
        assertThat(found.get().getClaimId()).isEqualTo("CLM-002");
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        Optional<RiskScore> found = riskScoreRepository.findById(999L);
        assertThat(found).isEmpty();
    }

    // ── Custom Queries ───────────────────────────────────────────────────────

    @Test
    void findByClaimId_ShouldReturnMatchingScores() {
        List<RiskScore> result = riskScoreRepository.findByClaimId("CLM-001");
        assertThat(result).hasSize(2);
        result.forEach(s -> assertThat(s.getClaimId()).isEqualTo("CLM-001"));
    }

    @Test
    void findByClaimId_WhenNotExists_ShouldReturnEmptyList() {
        List<RiskScore> result = riskScoreRepository.findByClaimId("CLM-999");
        assertThat(result).isEmpty();
    }

    @Test
    void findTopByClaimIdOrderByComputedDateDesc_ShouldReturnLatestScore() {
        Optional<RiskScore> latest = riskScoreRepository
                .findTopByClaimIdOrderByComputedDateDesc("CLM-001");

        assertThat(latest).isPresent();
        assertThat(latest.get().getComputedDate()).isEqualTo(LocalDate.of(2024, 3, 20));
        assertThat(latest.get().getScoreValue()).isEqualTo(90.0);
    }

    @Test
    void findTopByClaimIdOrderByComputedDateDesc_WhenNotExists_ShouldReturnEmpty() {
        Optional<RiskScore> result = riskScoreRepository
                .findTopByClaimIdOrderByComputedDateDesc("CLM-999");
        assertThat(result).isEmpty();
    }

    @Test
    void findByScoreValueGreaterThanEqual_ShouldReturnMatchingScores() {
        List<RiskScore> result = riskScoreRepository.findByScoreValueGreaterThanEqual(80.0);
        assertThat(result).hasSize(2);
        result.forEach(s -> assertThat(s.getScoreValue()).isGreaterThanOrEqualTo(80.0));
    }

    @Test
    void findByScoreValueGreaterThanEqual_WhenNoneMatch_ShouldReturnEmptyList() {
        List<RiskScore> result = riskScoreRepository.findByScoreValueGreaterThanEqual(99.9);
        assertThat(result).isEmpty();
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_ShouldRemoveScore() {
        RiskScore saved = riskScoreRepository.findByClaimId("CLM-002").get(0);
        riskScoreRepository.delete(saved);

        Optional<RiskScore> deleted = riskScoreRepository.findById(saved.getScoreId());
        assertThat(deleted).isEmpty();
    }
}