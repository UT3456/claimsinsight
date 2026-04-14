package com.claims.repository;

import com.claims.entity.ClaimReserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClaimReserveRepository extends JpaRepository<ClaimReserve, Long> {

    // Find all reserve entries for a specific claim
    List<ClaimReserve> findByClaimId(String claimId);

    // Find all reserves updated on a specific date
    List<ClaimReserve> findByUpdatedDate(LocalDate updatedDate);

    // Find all reserves updated within a date range
    List<ClaimReserve> findByUpdatedDateBetween(LocalDate startDate, LocalDate endDate);

    // Get the latest reserve entry for a claim (most recently updated)
    @Query("SELECT r FROM ClaimReserve r WHERE r.claimId = :claimId ORDER BY r.updatedDate DESC")
    List<ClaimReserve> findLatestReserveByClaimId(String claimId);

    // Get total reserved amount across all claims
    @Query("SELECT COALESCE(SUM(r.reserveAmount), 0) FROM ClaimReserve r")
    BigDecimal getTotalReserveAmount();
}
