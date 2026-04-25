package com.claiminsight.metrics.repository;

import com.claiminsight.metrics.model.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimStatusRepository extends JpaRepository<ClaimStatus, String> {
}
