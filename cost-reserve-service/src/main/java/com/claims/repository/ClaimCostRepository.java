package com.claims.repository;

import com.claims.entity.ClaimCost;
import com.claims.enums.CostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClaimCostRepository extends JpaRepository<ClaimCost, Long> {

    // Find all costs for a specific claim
    List<ClaimCost> findByClaimId(String claimId);

    // Find all costs by cost type (e.g. all MEDICAL costs)
    List<ClaimCost> findByCostType(CostType costType);

    // Find all costs for a claim filtered by cost type
    List<ClaimCost> findByClaimIdAndCostType(String claimId, CostType costType);

    // Find all costs recorded on a specific date
    List<ClaimCost> findByCostDate(LocalDate costDate);

    // Find all costs within a date range
    List<ClaimCost> findByCostDateBetween(LocalDate startDate, LocalDate endDate);

    // Get total amount spent for a specific claim
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM ClaimCost c WHERE c.claimId = :claimId")
    BigDecimal getTotalAmountByClaimId(String claimId);
}
