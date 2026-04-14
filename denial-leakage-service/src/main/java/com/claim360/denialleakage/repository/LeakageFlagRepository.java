package com.claim360.denialleakage.repository;

import com.claim360.denialleakage.entity.LeakageFlag;
import com.claim360.denialleakage.enums.LeakageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeakageFlagRepository extends JpaRepository<LeakageFlag, Long> {

    // Findspecific claim
    List<LeakageFlag> findByClaimId(String claimId);

    // Find leakage type
    List<LeakageFlag> findByLeakageType(LeakageType leakageType);

    // Find estimated loss greater than or equal to amount
    List<LeakageFlag> findByEstimatedLossGreaterThanEqual(Double amount);

    // Findby claim and leakage type
    List<LeakageFlag> findByClaimIdAndLeakageType(String claimId, LeakageType leakageType);
}