package com.claims.repositories;

import com.claims.entity.ClaimReserve;
import com.claims.repository.ClaimReserveRepository;
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
class ClaimReserveRepositoryTest {

    @Autowired
    private ClaimReserveRepository claimReserveRepository;

    @BeforeEach
    void setUp() {
        claimReserveRepository.deleteAll();

        claimReserveRepository.save(new ClaimReserve(null, "CLM001",
                new BigDecimal("10000.00"), LocalDate.of(2024, 1, 1)));
        claimReserveRepository.save(new ClaimReserve(null, "CLM001",
                new BigDecimal("12000.00"), LocalDate.of(2024, 3, 1)));
        claimReserveRepository.save(new ClaimReserve(null, "CLM001",
                new BigDecimal("15000.00"), LocalDate.of(2024, 6, 1)));
        claimReserveRepository.save(new ClaimReserve(null, "CLM002",
                new BigDecimal("8000.00"), LocalDate.of(2024, 2, 15)));
    }

    // -------------------------------------------------------
    // Save & Find by ID
    // -------------------------------------------------------

    @Test
    void testSaveClaimReserve_success() {
        ClaimReserve reserve = new ClaimReserve(null, "CLM003",
                new BigDecimal("5000.00"), LocalDate.now());
        ClaimReserve saved = claimReserveRepository.save(reserve);

        assertThat(saved.getReserveId()).isNotNull();
        assertThat(saved.getClaimId()).isEqualTo("CLM003");
        assertThat(saved.getReserveAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void testFindById_success() {
        ClaimReserve reserve = claimReserveRepository.save(new ClaimReserve(null,
                "CLM004", new BigDecimal("3000.00"), LocalDate.now()));

        assertThat(claimReserveRepository.findById(reserve.getReserveId())).isPresent();
    }

    @Test
    void testFindById_notFound() {
        assertThat(claimReserveRepository.findById(999L)).isEmpty();
    }

    // -------------------------------------------------------
    // Find by ClaimId
    // -------------------------------------------------------

    @Test
    void testFindByClaimId_returnsThreeRecords() {
        List<ClaimReserve> reserves = claimReserveRepository.findByClaimId("CLM001");
        assertThat(reserves).hasSize(3);
    }

    @Test
    void testFindByClaimId_noMatch_returnsEmpty() {
        List<ClaimReserve> reserves = claimReserveRepository.findByClaimId("CLM999");
        assertThat(reserves).isEmpty();
    }

    // -------------------------------------------------------
    // Find by Date Range
    // -------------------------------------------------------

    @Test
    void testFindByUpdatedDateBetween_returnsCorrectRecords() {
        List<ClaimReserve> reserves = claimReserveRepository.findByUpdatedDateBetween(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));
        assertThat(reserves).hasSize(3);
    }

    @Test
    void testFindByUpdatedDateBetween_noMatch_returnsEmpty() {
        List<ClaimReserve> reserves = claimReserveRepository.findByUpdatedDateBetween(
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        assertThat(reserves).isEmpty();
    }

    // -------------------------------------------------------
    // Find Latest Reserve by ClaimId
    // -------------------------------------------------------

    @Test
    void testFindLatestReserveByClaimId_returnsLatestFirst() {
        List<ClaimReserve> reserves = claimReserveRepository.findLatestReserveByClaimId("CLM001");

        assertThat(reserves).isNotEmpty();
        // First result should be the most recently updated one
        assertThat(reserves.get(0).getReserveAmount()).isEqualByComparingTo("15000.00");
        assertThat(reserves.get(0).getUpdatedDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    void testFindLatestReserveByClaimId_noMatch_returnsEmpty() {
        List<ClaimReserve> reserves = claimReserveRepository.findLatestReserveByClaimId("CLM999");
        assertThat(reserves).isEmpty();
    }

    // -------------------------------------------------------
    // Get Total Reserve Amount
    // -------------------------------------------------------

    @Test
    void testGetTotalReserveAmount_returnsCorrectSum() {
        // 10000 + 12000 + 15000 + 8000 = 45000
        BigDecimal total = claimReserveRepository.getTotalReserveAmount();
        assertThat(total).isEqualByComparingTo("45000.00");
    }

    // -------------------------------------------------------
    // Update & Delete
    // -------------------------------------------------------

    @Test
    void testUpdateClaimReserve_success() {
        ClaimReserve reserve = claimReserveRepository.findByClaimId("CLM002").get(0);
        reserve.setReserveAmount(new BigDecimal("9999.00"));
        ClaimReserve updated = claimReserveRepository.save(reserve);

        assertThat(updated.getReserveAmount()).isEqualByComparingTo("9999.00");
    }

    @Test
    void testDeleteClaimReserve_success() {
        ClaimReserve reserve = claimReserveRepository.findByClaimId("CLM002").get(0);
        Long id = reserve.getReserveId();
        claimReserveRepository.delete(reserve);

        assertThat(claimReserveRepository.findById(id)).isEmpty();
    }
}