package com.claims.repositories;

import com.claims.entity.ClaimCost;
import com.claims.enums.CostType;
import com.claims.repository.ClaimCostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ClaimCostRepositoryTest {

    @Autowired
    private ClaimCostRepository claimCostRepository;

    @BeforeEach
    void setUp() {
        claimCostRepository.deleteAll();

        claimCostRepository.save(new ClaimCost(null, "CLM001", CostType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.of(2024, 1, 15)));
        claimCostRepository.save(new ClaimCost(null, "CLM001", CostType.LEGAL,
                new BigDecimal("2000.00"), LocalDate.of(2024, 2, 10)));
        claimCostRepository.save(new ClaimCost(null, "CLM002", CostType.MEDICAL,
                new BigDecimal("3000.00"), LocalDate.of(2024, 3, 5)));
        claimCostRepository.save(new ClaimCost(null, "CLM002", CostType.REPAIR,
                new BigDecimal("1500.00"), LocalDate.of(2024, 4, 20)));
    }

    // -------------------------------------------------------
    // Save & Find by ID
    // -------------------------------------------------------

    @Test
    void testSaveClaimCost_success() {
        ClaimCost cost = new ClaimCost(null, "CLM003", CostType.SETTLEMENT,
                new BigDecimal("8000.00"), LocalDate.of(2024, 5, 1));
        ClaimCost saved = claimCostRepository.save(cost);

        assertThat(saved.getCostId()).isNotNull();
        assertThat(saved.getClaimId()).isEqualTo("CLM003");
        assertThat(saved.getCostType()).isEqualTo(CostType.SETTLEMENT);
        assertThat(saved.getAmount()).isEqualByComparingTo("8000.00");
    }

    @Test
    void testFindById_success() {
        ClaimCost cost = claimCostRepository.save(new ClaimCost(null, "CLM004",
                CostType.MEDICAL, new BigDecimal("1000.00"), LocalDate.now()));

        assertThat(claimCostRepository.findById(cost.getCostId())).isPresent();
    }

    @Test
    void testFindById_notFound() {
        assertThat(claimCostRepository.findById(999L)).isEmpty();
    }

    // -------------------------------------------------------
    // Find by ClaimId
    // -------------------------------------------------------

    @Test
    void testFindByClaimId_returnsTwoRecords() {
        List<ClaimCost> costs = claimCostRepository.findByClaimId("CLM001");
        assertThat(costs).hasSize(2);
    }

    @Test
    void testFindByClaimId_noMatch_returnsEmpty() {
        List<ClaimCost> costs = claimCostRepository.findByClaimId("CLM999");
        assertThat(costs).isEmpty();
    }

    // -------------------------------------------------------
    // Find by CostType
    // -------------------------------------------------------

    @Test
    void testFindByCostType_medical_returnsTwoRecords() {
        List<ClaimCost> costs = claimCostRepository.findByCostType(CostType.MEDICAL);
        assertThat(costs).hasSize(2);
        assertThat(costs).allMatch(c -> c.getCostType() == CostType.MEDICAL);
    }

    @Test
    void testFindByCostType_settlement_returnsEmpty() {
        List<ClaimCost> costs = claimCostRepository.findByCostType(CostType.SETTLEMENT);
        assertThat(costs).isEmpty();
    }

    // -------------------------------------------------------
    // Find by ClaimId and CostType
    // -------------------------------------------------------

    @Test
    void testFindByClaimIdAndCostType_success() {
        List<ClaimCost> costs = claimCostRepository.findByClaimIdAndCostType("CLM001", CostType.MEDICAL);
        assertThat(costs).hasSize(1);
        assertThat(costs.get(0).getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void testFindByClaimIdAndCostType_noMatch_returnsEmpty() {
        List<ClaimCost> costs = claimCostRepository.findByClaimIdAndCostType("CLM001", CostType.REPAIR);
        assertThat(costs).isEmpty();
    }

    // -------------------------------------------------------
    // Find by Date Range
    // -------------------------------------------------------

    @Test
    void testFindByCostDateBetween_returnsCorrectRecords() {
        List<ClaimCost> costs = claimCostRepository.findByCostDateBetween(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 28));
        assertThat(costs).hasSize(2);
    }

    @Test
    void testFindByCostDateBetween_noMatch_returnsEmpty() {
        List<ClaimCost> costs = claimCostRepository.findByCostDateBetween(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        assertThat(costs).isEmpty();
    }

    // -------------------------------------------------------
    // Get Total Amount by ClaimId
    // -------------------------------------------------------

    @Test
    void testGetTotalAmountByClaimId_returnsCorrectSum() {
        BigDecimal total = claimCostRepository.getTotalAmountByClaimId("CLM001");
        assertThat(total).isEqualByComparingTo("7000.00");
    }

    @Test
    void testGetTotalAmountByClaimId_noRecords_returnsZero() {
        BigDecimal total = claimCostRepository.getTotalAmountByClaimId("CLM999");
        assertThat(total).isEqualByComparingTo("0");
    }

    // -------------------------------------------------------
    // Update & Delete
    // -------------------------------------------------------

    @Test
    void testUpdateClaimCost_success() {
        ClaimCost cost = claimCostRepository.findByClaimId("CLM001").get(0);
        cost.setAmount(new BigDecimal("9999.00"));
        ClaimCost updated = claimCostRepository.save(cost);

        assertThat(updated.getAmount()).isEqualByComparingTo("9999.00");
    }

    @Test
    void testDeleteClaimCost_success() {
        ClaimCost cost = claimCostRepository.findByClaimId("CLM001").get(0);
        Long id = cost.getCostId();
        claimCostRepository.delete(cost);

        assertThat(claimCostRepository.findById(id)).isEmpty();
    }
}