package com.claims.repository;

import com.claims.enums.AgingBucket;
import com.claims.entity.AgingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgingRecordRepository extends JpaRepository<AgingRecord, Long> {

    // Find all aging records for a specific claim
    List<AgingRecord> findByClaimId(String claimId);

    // Find all records that fall in a specific aging bucket
    List<AgingRecord> findByAgingBucket(AgingBucket agingBucket);

    // Find aging record for a specific claim in a specific bucket
    Optional<AgingRecord> findByClaimIdAndAgingBucket(String claimId, AgingBucket agingBucket);

    // Find all claims aged more than a given number of days
    List<AgingRecord> findByAgingDaysGreaterThan(Integer days);

    // Count how many claims fall in each bucket (for trend/summary reporting)
    @Query("SELECT a.agingBucket, COUNT(a) FROM AgingRecord a GROUP BY a.agingBucket")
    List<Object[]> countByAgingBucket();
}
