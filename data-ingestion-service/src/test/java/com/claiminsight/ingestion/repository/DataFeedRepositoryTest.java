package com.claiminsight.ingestion.repository;

import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DataFeedRepositoryTest {

    @Autowired DataFeedRepository dataFeedRepository;

    private DataFeed buildFeed(FeedType type, String source, FeedStatus status) {
        DataFeed feed = new DataFeed();
        feed.setFeedType(type);
        feed.setSourceSystem(source);
        feed.setStatus(status);
        return feed;
    }

    @Test
    void save_assignsId() {
        DataFeed saved = dataFeedRepository.save(buildFeed(FeedType.CLAIM, "ClaimsPro", FeedStatus.ACTIVE));

        assertNotNull(saved.getFeedId());
    }

    @Test
    void findByFeedType_returnsMatching() {
        dataFeedRepository.save(buildFeed(FeedType.CLAIM, "System A", FeedStatus.ACTIVE));
        dataFeedRepository.save(buildFeed(FeedType.POLICY, "System B", FeedStatus.ACTIVE));

        List<DataFeed> result = dataFeedRepository.findByFeedType(FeedType.CLAIM);

        assertEquals(1, result.size());
        assertEquals(FeedType.CLAIM, result.get(0).getFeedType());
    }

    @Test
    void findByStatus_returnsMatching() {
        dataFeedRepository.save(buildFeed(FeedType.CLAIM, "System A", FeedStatus.ACTIVE));
        dataFeedRepository.save(buildFeed(FeedType.POLICY, "System B", FeedStatus.INACTIVE));

        List<DataFeed> result = dataFeedRepository.findByStatus(FeedStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void deleteById_removesRecord() {
        DataFeed saved = dataFeedRepository.save(buildFeed(FeedType.CLAIM, "System A", FeedStatus.ACTIVE));

        dataFeedRepository.deleteById(saved.getFeedId());

        assertFalse(dataFeedRepository.findById(saved.getFeedId()).isPresent());
    }
}
