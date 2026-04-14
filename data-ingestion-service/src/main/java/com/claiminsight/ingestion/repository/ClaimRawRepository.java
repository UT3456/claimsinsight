package com.claiminsight.ingestion.repository;

import com.claiminsight.ingestion.model.ClaimRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data JPA repository for ClaimRaw entities. */
@Repository
public interface ClaimRawRepository extends JpaRepository<ClaimRaw, Long> {

    
    List<ClaimRaw> findByClaimId(String claimId);

    
    List<ClaimRaw> findByDataFeed_FeedId(Long feedId);
}
