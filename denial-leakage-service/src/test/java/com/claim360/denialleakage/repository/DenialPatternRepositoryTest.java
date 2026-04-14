package com.claim360.denialleakage.repository;

import com.claim360.denialleakage.entity.DenialPattern;
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
class DenialPatternRepositoryTest {

    @Autowired
    private DenialPatternRepository denialPatternRepository;

    private DenialPattern pattern1;
    private DenialPattern pattern2;

    @BeforeEach
    void setUp() {
        denialPatternRepository.deleteAll();

        pattern1 = new DenialPattern();
        pattern1.setClaimId("CLM-001");
        pattern1.setDenialCode("CO-4");
        pattern1.setReason("Service not covered");
        pattern1.setOccurrenceDate(LocalDate.of(2024, 1, 10));

        pattern2 = new DenialPattern();
        pattern2.setClaimId("CLM-002");
        pattern2.setDenialCode("PR-96");
        pattern2.setReason("Non-covered charge");
        pattern2.setOccurrenceDate(LocalDate.of(2024, 2, 20));

        denialPatternRepository.save(pattern1);
        denialPatternRepository.save(pattern2);
    }

    //Save & Find
    @Test
    void save_ShouldPersistDenialPattern() {
        List<DenialPattern> all = denialPatternRepository.findAll();
        assertThat(all.get(0).getPatternId()).isNotNull();
        assertThat(all.get(0).getClaimId()).isNotNull();
    }

    @Test
    void findAll_ShouldReturnAllPatterns() {
        List<DenialPattern> all = denialPatternRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void findById_WhenExists_ShouldReturnPattern() {
        DenialPattern saved = denialPatternRepository.findByClaimId("CLM-001").get(0);
        Optional<DenialPattern> found = denialPatternRepository.findById(saved.getPatternId());
        assertThat(found).isPresent();
        assertThat(found.get().getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        Optional<DenialPattern> found = denialPatternRepository.findById(999L);
        assertThat(found).isEmpty();
    }

    //Custom
    @Test
    void findByClaimId_ShouldReturnMatchingPatterns() {
        List<DenialPattern> result = denialPatternRepository.findByClaimId("CLM-001");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo("CLM-001");
    }

    @Test
    void findByClaimId_WhenNotExists_ShouldReturnEmptyList() {
        List<DenialPattern> result = denialPatternRepository.findByClaimId("CLM-999");
        assertThat(result).isEmpty();
    }

    @Test
    void findByDenialCode_ShouldReturnMatchingPatterns() {
        List<DenialPattern> result = denialPatternRepository.findByDenialCode("CO-4");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDenialCode()).isEqualTo("CO-4");
    }

    @Test
    void findByDenialCode_WhenNotExists_ShouldReturnEmptyList() {
        List<DenialPattern> result = denialPatternRepository.findByDenialCode("XX-99");
        assertThat(result).isEmpty();
    }

    @Test
    void findByReasonContainingIgnoreCase_ShouldReturnMatchingPatterns() {
        List<DenialPattern> result = denialPatternRepository
                .findByReasonContainingIgnoreCase("covered");
        assertThat(result).hasSize(2);
    }

    @Test
    void findByReasonContainingIgnoreCase_WhenNotExists_ShouldReturnEmptyList() {
        List<DenialPattern> result = denialPatternRepository
                .findByReasonContainingIgnoreCase("xyz");
        assertThat(result).isEmpty();
    }

    @Test
    void findByClaimIdAndDenialCode_ShouldReturnMatchingPatterns() {
        List<DenialPattern> result = denialPatternRepository
                .findByClaimIdAndDenialCode("CLM-001", "CO-4");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo("CLM-001");
        assertThat(result.get(0).getDenialCode()).isEqualTo("CO-4");
    }

    //Delet

    @Test
    void delete_ShouldRemovePattern() {
        DenialPattern saved = denialPatternRepository.findByClaimId("CLM-001").get(0);
        denialPatternRepository.delete(saved);

        Optional<DenialPattern> deleted = denialPatternRepository.findById(saved.getPatternId());
        assertThat(deleted).isEmpty();
    }
}