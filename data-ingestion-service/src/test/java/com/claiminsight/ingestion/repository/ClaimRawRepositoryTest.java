package com.claiminsight.ingestion.repository;

import com.claiminsight.ingestion.model.ClaimRaw;
import com.claiminsight.ingestion.model.DataFeed;
import com.claiminsight.ingestion.model.FeedStatus;
import com.claiminsight.ingestion.model.FeedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClaimRawRepositoryTest {

    @Autowired ClaimRawRepository claimRawRepository;
    @Autowired DataFeedRepository dataFeedRepository;

    private DataFeed feed;

    @BeforeEach
    void setUp() {
        DataFeed f = new DataFeed();
        f.setFeedType(FeedType.CLAIM);
        f.setSourceSystem("ClaimsPro");
        f.setStatus(FeedStatus.ACTIVE);
        feed = dataFeedRepository.save(f);
    }

    private ClaimRaw buildRaw(String claimId) {
        ClaimRaw raw = new ClaimRaw();
        raw.setClaimId(claimId);
        raw.setDataFeed(feed);
        raw.setPayloadJson("{\"claimId\":\"" + claimId + "\"}");
        return raw;
    }

    @Test
    void save_assignsId() {
        ClaimRaw saved = claimRawRepository.save(buildRaw("CLM-001"));

        assertNotNull(saved.getRawId());
    }

    @Test
    void findByClaimId_returnsMatching() {
        claimRawRepository.save(buildRaw("CLM-001"));
        claimRawRepository.save(buildRaw("CLM-002"));

        List<ClaimRaw> result = claimRawRepository.findByClaimId("CLM-001");

        assertEquals(1, result.size());
        assertEquals("CLM-001", result.get(0).getClaimId());
    }

    @Test
    void findByDataFeed_FeedId_returnsMatching() {
        claimRawRepository.save(buildRaw("CLM-001"));
        claimRawRepository.save(buildRaw("CLM-002"));

        List<ClaimRaw> result = claimRawRepository.findByDataFeed_FeedId(feed.getFeedId());

        assertEquals(2, result.size());
    }

    @Test
    void deleteById_removesRecord() {
        ClaimRaw saved = claimRawRepository.save(buildRaw("CLM-001"));

        claimRawRepository.deleteById(saved.getRawId());

        assertFalse(claimRawRepository.findById(saved.getRawId()).isPresent());
    }
}
