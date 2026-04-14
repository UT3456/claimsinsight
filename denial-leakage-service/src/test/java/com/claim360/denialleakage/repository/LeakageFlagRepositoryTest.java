package com.claim360.denialleakage.repository;

import com.claim360.denialleakage.entity.LeakageFlag;
import com.claim360.denialleakage.enums.LeakageType;
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
class LeakageFlagRepositoryTest {

    @Autowired
    private LeakageFlagRepository leakageFlagRepository;

    private LeakageFlag flag1;
    private LeakageFlag flag2;
    private LeakageFlag flag3;

    @BeforeEach
    void setUp() {
        leakageFlagRepository.deleteAll();

        flag1 = new LeakageFlag();
        flag1.setClaimId("CLM-001");
        flag1.setLeakageType(LeakageType.Overpayment);
        flag1.setEstimatedLoss(5000.0);
        flag1.setIdentifiedDate(LocalDate.of(2024, 1, 10));

        flag2 = new LeakageFlag();
        flag2.setClaimId("CLM-001");
        flag2.setLeakageType(LeakageType.Delay);
        flag2.setEstimatedLoss(1500.0);
        flag2.setIdentifiedDate(LocalDate.of(2024, 2, 15));

        flag3 = new LeakageFlag();
        flag3.setClaimId("CLM-002");
        flag3.setLeakageType(LeakageType.Error);
        flag3.setEstimatedLoss(800.0);
        flag3.setIdentifiedDate(LocalDate.of(2024, 3, 20));

        leakageFlagRepository.save(flag1);
        leakageFlagRepository.save(flag2);
        leakageFlagRepository.save(flag3);
    }

    //Save & Find
    @Test
    void save_ShouldPersistLeakageFlag() {
        List<LeakageFlag> all = leakageFlagRepository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.get(0).getLeakageId()).isNotNull();
    }

    @Test
    void findAll_ShouldReturnAllFlags() {
        List<LeakageFlag> all = leakageFlagRepository.findAll();
        assertThat(all).hasSize(3);
    }

    @Test
    void findById_WhenExists_ShouldReturnFlag() {
        LeakageFlag saved = leakageFlagRepository.findByClaimId("CLM-002").get(0);
        Optional<LeakageFlag> found = leakageFlagRepository.findById(saved.getLeakageId());
        assertThat(found).isPresent();
        assertThat(found.get().getClaimId()).isEqualTo("CLM-002");
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        Optional<LeakageFlag> found = leakageFlagRepository.findById(999L);
        assertThat(found).isEmpty();
    }

    //Custom Queries
    @Test
    void findByClaimId_ShouldReturnMatchingFlags() {
        List<LeakageFlag> result = leakageFlagRepository.findByClaimId("CLM-001");
        assertThat(result).hasSize(2);
        result.forEach(f -> assertThat(f.getClaimId()).isEqualTo("CLM-001"));
    }

    @Test
    void findByClaimId_WhenNotExists_ShouldReturnEmptyList() {
        List<LeakageFlag> result = leakageFlagRepository.findByClaimId("CLM-999");
        assertThat(result).isEmpty();
    }

    @Test
    void findByLeakageType_ShouldReturnMatchingFlags() {
        List<LeakageFlag> result = leakageFlagRepository.findByLeakageType(LeakageType.Overpayment);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLeakageType()).isEqualTo(LeakageType.Overpayment);
    }

    @Test
    void findByLeakageType_WhenNotExists_ShouldReturnEmptyList() {
        List<LeakageFlag> result = leakageFlagRepository.findByLeakageType(LeakageType.Error);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLeakageType()).isEqualTo(LeakageType.Error);
    }

    @Test
    void findByEstimatedLossGreaterThanEqual_ShouldReturnMatchingFlags() {
        List<LeakageFlag> result = leakageFlagRepository
                .findByEstimatedLossGreaterThanEqual(1500.0);
        assertThat(result).hasSize(2);
        result.forEach(f -> assertThat(f.getEstimatedLoss()).isGreaterThanOrEqualTo(1500.0));
    }

    @Test
    void findByEstimatedLossGreaterThanEqual_WhenNoneMatch_ShouldReturnEmptyList() {
        List<LeakageFlag> result = leakageFlagRepository
                .findByEstimatedLossGreaterThanEqual(99999.0);
        assertThat(result).isEmpty();
    }

    @Test
    void findByClaimIdAndLeakageType_ShouldReturnMatchingFlags() {
        List<LeakageFlag> result = leakageFlagRepository
                .findByClaimIdAndLeakageType("CLM-001", LeakageType.Overpayment);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo("CLM-001");
        assertThat(result.get(0).getLeakageType()).isEqualTo(LeakageType.Overpayment);
    }

    //Delete
    @Test
    void delete_ShouldRemoveFlag() {
        LeakageFlag saved = leakageFlagRepository.findByClaimId("CLM-002").get(0);
        leakageFlagRepository.delete(saved);

        Optional<LeakageFlag> deleted = leakageFlagRepository.findById(saved.getLeakageId());
        assertThat(deleted).isEmpty();
    }
}