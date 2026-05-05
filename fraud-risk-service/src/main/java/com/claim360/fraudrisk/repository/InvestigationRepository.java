package com.claim360.fraudrisk.repository;

import com.claim360.fraudrisk.entity.Investigation;
import com.claim360.fraudrisk.enums.InvestigationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, Long> {

    List<Investigation> findByClaimIdOrderByOpenedAtDesc(String claimId);

    List<Investigation> findByStatusOrderByOpenedAtDesc(InvestigationStatus status);

    List<Investigation> findByAssignedToOrderByOpenedAtDesc(String assignedTo);

    long countByStatus(InvestigationStatus status);

    /** Prevent two analysts from opening parallel investigations on the same claim. */
    boolean existsByClaimIdAndStatusIn(String claimId, List<InvestigationStatus> openStatuses);
}
