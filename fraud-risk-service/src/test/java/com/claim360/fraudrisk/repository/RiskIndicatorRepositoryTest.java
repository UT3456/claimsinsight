package com.claim360.fraudrisk.repository;

import com.claim360.fraudrisk.entity.RiskIndicator;
import com.claim360.fraudrisk.enums.IndicatorType;
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
class RiskIndicatorRepositoryTest {

    @Autowired
    private RiskIndicatorRepository riskIndicatorRepository;

    private RiskIndicator indicator1;
    private RiskIndicator indicator2;

    @BeforeEach
    void setUp() {
        riskIndicatorRepository.deleteAll();

        indicator1 = new RiskIndicator();
        indicator1.setClaimId("CLM-001");
        indicator1.setIndicatorType(IndicatorType.HighCost);
        indicator1.setSeverity("HIGH");
        indicator1.setTriggeredDate(LocalDate.of(2024, 1, 10));

        indicator2 = new RiskIndicator();
        indicator2.setClaimId("CLM-002");
        indicator2.setIndicatorType(IndicatorType.UnusualTiming);
        indicator2.setSeverity("LOW");
        indicator2.setTriggeredDate(LocalDate.of(2024, 2, 20));

        riskIndicatorRepository.save(indicator1);
        riskIndicatorRepository.save(indicator2);
    }

    // ── Save & Find ──────────────────────────────────────────────────────────

    @Test
    void save_ShouldPersistRiskIndicator() {
        RiskIndicator saved = riskIndicatorRepository.findAll().get(0);
        assertThat(saved.getIndicatorId()).isNotNull();
        assertThat(saved.getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    void findAll_ShouldReturnAllIndicators() {
        List<RiskIndicator> all = riskIndicatorRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void findById_WhenExists_ShouldReturnIndicator() {
        RiskIndicator saved = riskIndicatorRepository.save(indicator1);
        Optional<RiskIndicator> found = riskIndicatorRepository.findById(saved.getIndicatorId());
        assertThat(found).isPresent();
        assertThat(found.get().getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        Optional<RiskIndicator> found = riskIndicatorRepository.findById(999L);
        assertThat(found).isEmpty();
    }

    // ── Custom Queries ───────────────────────────────────────────────────────

    @Test
    void findByClaimId_ShouldReturnMatchingIndicators() {
        List<RiskIndicator> result = riskIndicatorRepository.findByClaimId("CLM-001");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    void findByClaimId_WhenNotExists_ShouldReturnEmptyList() {
        List<RiskIndicator> result = riskIndicatorRepository.findByClaimId("CLM-999");
        assertThat(result).isEmpty();
    }

    @Test
    void findByIndicatorType_ShouldReturnMatchingIndicators() {
        List<RiskIndicator> result = riskIndicatorRepository.findByIndicatorType(IndicatorType.HighCost);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIndicatorType()).isEqualTo(IndicatorType.HighCost);
    }

    @Test
    void findBySeverity_ShouldReturnMatchingIndicators() {
        List<RiskIndicator> result = riskIndicatorRepository.findBySeverity("HIGH");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void findByClaimIdAndIndicatorType_ShouldReturnMatchingIndicators() {
        List<RiskIndicator> result = riskIndicatorRepository
                .findByClaimIdAndIndicatorType("CLM-001", IndicatorType.HighCost);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo("CLM-001");
        assertThat(result.get(0).getIndicatorType()).isEqualTo(IndicatorType.HighCost);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_ShouldRemoveIndicator() {
        RiskIndicator saved = riskIndicatorRepository.findByClaimId("CLM-001").get(0);
        riskIndicatorRepository.delete(saved);

        Optional<RiskIndicator> deleted = riskIndicatorRepository.findById(saved.getIndicatorId());
        assertThat(deleted).isEmpty();
    }
}