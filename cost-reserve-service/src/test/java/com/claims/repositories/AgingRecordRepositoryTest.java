package com.claims.repositories;

import com.claims.enums.AgingBucket;
import com.claims.entity.AgingRecord;
import com.claims.repository.AgingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AgingRecordRepositoryTest {

    @Autowired
    private AgingRecordRepository agingRecordRepository;

    @BeforeEach
    void setUp() {
        agingRecordRepository.deleteAll();

        agingRecordRepository.save(new AgingRecord(null, "CLM001", 15, AgingBucket.BUCKET_0_30));
        agingRecordRepository.save(new AgingRecord(null, "CLM002", 45, AgingBucket.BUCKET_31_60));
        agingRecordRepository.save(new AgingRecord(null, "CLM003", 75, AgingBucket.BUCKET_61_90));
        agingRecordRepository.save(new AgingRecord(null, "CLM004", 100, AgingBucket.BUCKET_90_PLUS));
        agingRecordRepository.save(new AgingRecord(null, "CLM005", 110, AgingBucket.BUCKET_90_PLUS));
    }

    // -------------------------------------------------------
    // Save & Find by ID
    // -------------------------------------------------------

    @Test
    void testSaveAgingRecord_success() {
        AgingRecord record = new AgingRecord(null, "CLM006", 20, AgingBucket.BUCKET_0_30);
        AgingRecord saved = agingRecordRepository.save(record);

        assertThat(saved.getAgingId()).isNotNull();
        assertThat(saved.getClaimId()).isEqualTo("CLM006");
        assertThat(saved.getAgingDays()).isEqualTo(20);
        assertThat(saved.getAgingBucket()).isEqualTo(AgingBucket.BUCKET_0_30);
    }

    @Test
    void testFindById_success() {
        AgingRecord record = agingRecordRepository.save(
                new AgingRecord(null, "CLM007", 30, AgingBucket.BUCKET_0_30));

        assertThat(agingRecordRepository.findById(record.getAgingId())).isPresent();
    }

    @Test
    void testFindById_notFound() {
        assertThat(agingRecordRepository.findById(999L)).isEmpty();
    }

    // -------------------------------------------------------
    // Find by ClaimId
    // -------------------------------------------------------

    @Test
    void testFindByClaimId_success() {
        List<AgingRecord> records = agingRecordRepository.findByClaimId("CLM001");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getAgingDays()).isEqualTo(15);
    }

    @Test
    void testFindByClaimId_noMatch_returnsEmpty() {
        List<AgingRecord> records = agingRecordRepository.findByClaimId("CLM999");
        assertThat(records).isEmpty();
    }

    // -------------------------------------------------------
    // Find by AgingBucket
    // -------------------------------------------------------

    @Test
    void testFindByAgingBucket_bucket90Plus_returnsTwoRecords() {
        List<AgingRecord> records = agingRecordRepository.findByAgingBucket(AgingBucket.BUCKET_90_PLUS);
        assertThat(records).hasSize(2);
        assertThat(records).allMatch(r -> r.getAgingBucket() == AgingBucket.BUCKET_90_PLUS);
    }

    @Test
    void testFindByAgingBucket_bucket0_30_returnsOneRecord() {
        List<AgingRecord> records = agingRecordRepository.findByAgingBucket(AgingBucket.BUCKET_0_30);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getClaimId()).isEqualTo("CLM001");
    }

    // -------------------------------------------------------
    // Find by ClaimId and AgingBucket
    // -------------------------------------------------------

    @Test
    void testFindByClaimIdAndAgingBucket_success() {
        Optional<AgingRecord> record = agingRecordRepository
                .findByClaimIdAndAgingBucket("CLM002", AgingBucket.BUCKET_31_60);
        assertThat(record).isPresent();
        assertThat(record.get().getAgingDays()).isEqualTo(45);
    }

    @Test
    void testFindByClaimIdAndAgingBucket_noMatch_returnsEmpty() {
        Optional<AgingRecord> record = agingRecordRepository
                .findByClaimIdAndAgingBucket("CLM001", AgingBucket.BUCKET_90_PLUS);
        assertThat(record).isEmpty();
    }

    // -------------------------------------------------------
    // Find by AgingDays Greater Than
    // -------------------------------------------------------

    @Test
    void testFindByAgingDaysGreaterThan_60_returnsThreeRecords() {
        List<AgingRecord> records = agingRecordRepository.findByAgingDaysGreaterThan(60);
        assertThat(records).hasSize(3);
    }

    @Test
    void testFindByAgingDaysGreaterThan_100_returnsOneRecord() {
        List<AgingRecord> records = agingRecordRepository.findByAgingDaysGreaterThan(100);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getClaimId()).isEqualTo("CLM005");
    }

    @Test
    void testFindByAgingDaysGreaterThan_200_returnsEmpty() {
        List<AgingRecord> records = agingRecordRepository.findByAgingDaysGreaterThan(200);
        assertThat(records).isEmpty();
    }

    // -------------------------------------------------------
    // Count by AgingBucket
    // -------------------------------------------------------

    @Test
    void testCountByAgingBucket_returnsAllBuckets() {
        List<Object[]> results = agingRecordRepository.countByAgingBucket();
        assertThat(results).isNotEmpty();
        // Each result is [AgingBucket, count]
        assertThat(results).allSatisfy(row -> {
            assertThat(row[0]).isInstanceOf(AgingBucket.class);
            assertThat(row[1]).isInstanceOf(Long.class);
        });
    }

    @Test
    void testCountByAgingBucket_bucket90Plus_countIsTwo() {
        List<Object[]> results = agingRecordRepository.countByAgingBucket();
        results.stream()
                .filter(row -> row[0] == AgingBucket.BUCKET_90_PLUS)
                .findFirst()
                .ifPresent(row -> assertThat((Long) row[1]).isEqualTo(2L));
    }

    // -------------------------------------------------------
    // Update & Delete
    // -------------------------------------------------------

    @Test
    void testUpdateAgingRecord_success() {
        AgingRecord record = agingRecordRepository.findByClaimId("CLM001").get(0);
        record.setAgingDays(50);
        record.setAgingBucket(AgingBucket.BUCKET_31_60);
        AgingRecord updated = agingRecordRepository.save(record);

        assertThat(updated.getAgingDays()).isEqualTo(50);
        assertThat(updated.getAgingBucket()).isEqualTo(AgingBucket.BUCKET_31_60);
    }

    @Test
    void testDeleteAgingRecord_success() {
        AgingRecord record = agingRecordRepository.findByClaimId("CLM001").get(0);
        Long id = record.getAgingId();
        agingRecordRepository.delete(record);

        assertThat(agingRecordRepository.findById(id)).isEmpty();
    }
}