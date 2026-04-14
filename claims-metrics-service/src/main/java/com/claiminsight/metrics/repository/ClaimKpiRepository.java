package com.claiminsight.metrics.repository;

import com.claiminsight.metrics.model.ClaimKPI;
import com.claiminsight.metrics.model.MetricName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Spring Data JPA repository for ClaimKPI entities. */
@Repository
public interface ClaimKpiRepository extends JpaRepository<ClaimKPI, Long> {

    
    List<ClaimKPI> findByClaimId(String claimId);

    
    List<ClaimKPI> findByMetricName(MetricName metricName);

    
    List<ClaimKPI> findByClaimIdAndMetricName(String claimId, MetricName metricName);

    
    List<ClaimKPI> findByMetricDateBetween(LocalDate startDate, LocalDate endDate);
}
