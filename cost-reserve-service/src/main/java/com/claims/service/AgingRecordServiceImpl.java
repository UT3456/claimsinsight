package com.claims.service;

import com.claims.client.NotificationServiceClient;
import com.claims.client.dto.NotificationDispatchRequestDTO;
import com.claims.dto.request.AgingRecordRequest;
import com.claims.dto.response.AgingRecordResponse;
import com.claims.enums.AgingBucket;
import com.claims.entity.AgingRecord;
import com.claims.exception.ResourceNotFoundException;
import com.claims.repository.AgingRecordRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgingRecordServiceImpl implements AgingRecordService {

    private static final Logger log = LoggerFactory.getLogger(AgingRecordServiceImpl.class);

    private final AgingRecordRepository agingRecordRepository;
    private final ModelMapper modelMapper;

    /** Optional: null when NotificationService is unreachable. Notifications are non-blocking. */
    @Autowired(required = false)
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    public AgingRecordServiceImpl(AgingRecordRepository agingRecordRepository,
                                  ModelMapper modelMapper) {
        this.agingRecordRepository = agingRecordRepository;
        this.modelMapper = modelMapper;
    }




    @Override
    public AgingRecordResponse createAgingRecord(AgingRecordRequest request) {
        AgingRecord record = modelMapper.map(request, AgingRecord.class);
        record.setAgingBucket(deriveBucket(request.getAgingDays()));
        AgingRecord saved = agingRecordRepository.save(record);

        if (saved.getAgingBucket() == AgingBucket.BUCKET_90_PLUS) {
            sendCriticalAgingNotification(saved);
        }

        return modelMapper.map(saved, AgingRecordResponse.class);
    }

    private void sendCriticalAgingNotification(AgingRecord record) {
        if (notificationServiceClient == null) {
            log.debug("NotificationService unavailable — skipping aging alert for claim {}", record.getClaimId());
            return;
        }
        try {
            // Dispatch to every ANALYST (owns the claim) + MANAGER (escalation point)
            // instead of hardcoding userId=1L.
            NotificationDispatchRequestDTO notification = NotificationDispatchRequestDTO.builder()
                    .targetRoles(Set.of("ANALYST", "MANAGER"))
                    .title("Critical Aging Alert - Claim " + record.getClaimId())
                    .message("Claim " + record.getClaimId() + " has been aging for "
                            + record.getAgingDays() + " days and is in the BUCKET_90_PLUS category. Immediate action required.")
                    .category("AGING")
                    .referenceId(record.getClaimId())
                    .build();
            notificationServiceClient.dispatchNotification(notification);
            log.info("Dispatched AGING notification for claim {}", record.getClaimId());
        } catch (Exception e) {
            log.warn("Failed to dispatch critical aging notification for claim {}: {}", record.getClaimId(), e.getMessage());
        }
    }

    @Override
    public AgingRecordResponse getAgingRecordById(Long agingId) {
        AgingRecord record = findAgingByIdOrThrow(agingId);
        return modelMapper.map(record, AgingRecordResponse.class);
    }

    @Override
    public List<AgingRecordResponse> getAllAgingRecords() {
        return agingRecordRepository.findAll()
                .stream()
                .map(record -> modelMapper.map(record, AgingRecordResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public AgingRecordResponse updateAgingRecord(Long agingId, AgingRecordRequest request) {
        AgingRecord existing = findAgingByIdOrThrow(agingId);
        AgingBucket previousBucket = existing.getAgingBucket();
        modelMapper.map(request, existing);
        existing.setAgingBucket(deriveBucket(request.getAgingDays()));
        AgingRecord updated = agingRecordRepository.save(existing);

        // Only fire when the claim *transitions into* the critical bucket — avoids spam on every update
        if (updated.getAgingBucket() == AgingBucket.BUCKET_90_PLUS
                && previousBucket != AgingBucket.BUCKET_90_PLUS) {
            sendCriticalAgingNotification(updated);
        }

        return modelMapper.map(updated, AgingRecordResponse.class);
    }

    @Override
    public void deleteAgingRecord(Long agingId) {
        AgingRecord existing = findAgingByIdOrThrow(agingId);
        agingRecordRepository.delete(existing);
    }






    @Override
    public List<AgingRecordResponse> getAgingRecordsByClaimId(String claimId) {
        List<AgingRecord> records = agingRecordRepository.findByClaimId(claimId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No aging records found for claimId: " + claimId);
        }
        return records.stream()
                .map(record -> modelMapper.map(record, AgingRecordResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AgingRecordResponse> getAgingRecordsByBucket(AgingBucket agingBucket) {
        List<AgingRecord> records = agingRecordRepository.findByAgingBucket(agingBucket);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No aging records found for bucket: " + agingBucket);
        }
        return records.stream()
                .map(record -> modelMapper.map(record, AgingRecordResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AgingRecordResponse> getClaimsAgedBeyond(Integer days) {
        if (days < 0) {
            throw new IllegalArgumentException("Days threshold cannot be negative");
        }
        List<AgingRecord> records = agingRecordRepository.findByAgingDaysGreaterThan(days);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No claims found aged beyond " + days + " days");
        }
        return records.stream()
                .map(record -> modelMapper.map(record, AgingRecordResponse.class))
                .collect(Collectors.toList());
    }






    @Override
    public Map<String, Long> getAgingBucketDistribution() {
        List<Object[]> results = agingRecordRepository.countByAgingBucket();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No aging records found in the system");
        }
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (Object[] row : results) {
            AgingBucket bucket = (AgingBucket) row[0];
            Long count = (Long) row[1];
            distribution.put(bucket.name(), count);
        }
        return distribution;
    }

    @Override
    public Map<String, Object> getAgingSummaryForClaim(String claimId) {
        List<AgingRecord> records = agingRecordRepository.findByClaimId(claimId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No aging records found for claimId: " + claimId);
        }
        AgingRecord mostAged = records.stream()
                .max((a, b) -> Integer.compare(a.getAgingDays(), b.getAgingDays()))
                .orElseThrow();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("claimId", claimId);
        summary.put("agingDays", mostAged.getAgingDays());
        summary.put("agingBucket", mostAged.getAgingBucket().name());
        summary.put("escalationRequired", mostAged.getAgingDays() > 60);
        return summary;
    }

    @Override
    public Map<String, Object> getPortfolioAgingHealth() {
        List<AgingRecord> allRecords = agingRecordRepository.findAll();
        if (allRecords.isEmpty()) {
            throw new ResourceNotFoundException("No aging records found in the system");
        }
        long totalClaims = allRecords.size();
        double averageAgingDays = allRecords.stream()
                .mapToInt(AgingRecord::getAgingDays)
                .average()
                .orElse(0.0);
        long criticalClaims = allRecords.stream()
                .filter(r -> r.getAgingBucket() == AgingBucket.BUCKET_90_PLUS)
                .count();
        Map<String, String> bucketPercentages = new LinkedHashMap<>();
        for (AgingBucket bucket : AgingBucket.values()) {
            long count = allRecords.stream()
                    .filter(r -> r.getAgingBucket() == bucket)
                    .count();
            double percentage = (count * 100.0) / totalClaims;
            bucketPercentages.put(bucket.name(), String.format("%.1f%%", percentage));
        }
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("totalClaims", totalClaims);
        health.put("averageAgingDays", String.format("%.1f", averageAgingDays));
        health.put("criticalClaims_90Plus", criticalClaims);
        health.put("bucketDistributionPercentage", bucketPercentages);
        return health;
    }

    // ── Daily Aging Recompute ────────────────────────────────────────────────────

    /**
     * Runs daily at 01:00 and increments every {@link AgingRecord}'s {@code agingDays}
     * by 1, then re-derives the bucket. This keeps the aging data current without
     * requiring a new ingestion event for each claim.
     *
     * <p>When a record transitions into {@code BUCKET_90_PLUS} for the first time, a
     * critical aging notification is dispatched to ANALYST + MANAGER via the
     * {@link NotificationServiceClient} (fire-and-forget).</p>
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void computeAgingForAllClaims() {
        List<AgingRecord> all = agingRecordRepository.findAll();
        if (all.isEmpty()) {
            log.info("Daily aging recompute: no records found — skipping.");
            return;
        }
        log.info("Daily aging recompute starting for {} record(s)...", all.size());
        int updated = 0;
        for (AgingRecord record : all) {
            try {
                AgingBucket previousBucket = record.getAgingBucket();
                record.setAgingDays(record.getAgingDays() + 1);
                record.setAgingBucket(deriveBucket(record.getAgingDays()));
                agingRecordRepository.save(record);

                // Notify on bucket transition into critical territory (only once)
                if (record.getAgingBucket() == AgingBucket.BUCKET_90_PLUS
                        && previousBucket != AgingBucket.BUCKET_90_PLUS) {
                    sendCriticalAgingNotification(record);
                }
                updated++;
            } catch (Exception e) {
                log.warn("Aging recompute failed for agingId {}: {}", record.getAgingId(), e.getMessage());
            }
        }
        log.info("Daily aging recompute complete: {} of {} records updated", updated, all.size());
    }

    // -------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------

    private AgingBucket deriveBucket(int agingDays) {
        if (agingDays <= 30) return AgingBucket.BUCKET_0_30;
        else if (agingDays <= 60) return AgingBucket.BUCKET_31_60;
        else if (agingDays <= 90) return AgingBucket.BUCKET_61_90;
        else return AgingBucket.BUCKET_90_PLUS;
    }

    private AgingRecord findAgingByIdOrThrow(Long agingId) {
        return agingRecordRepository.findById(agingId)
                .orElseThrow(() -> new ResourceNotFoundException("AgingRecord", "agingId", agingId));
    }
}

