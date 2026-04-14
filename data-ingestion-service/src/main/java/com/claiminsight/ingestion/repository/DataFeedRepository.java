package com.claiminsight.ingestion.repository;

import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data JPA repository for DataFeed entities. */
@Repository
public interface DataFeedRepository extends JpaRepository<DataFeed, Long> {

    
    List<DataFeed> findByFeedType(FeedType feedType);

    
    List<DataFeed> findByStatus(FeedStatus status);
}
