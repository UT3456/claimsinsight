package com.claiminsight.metrics.service;

import com.claiminsight.metrics.dto.ClaimStatusResponseDTO;
import com.claiminsight.metrics.model.ClaimStatus;
import com.claiminsight.metrics.model.ClaimStatus.ClaimStatusValue;
import com.claiminsight.metrics.repository.ClaimStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reads and upserts ACTIVE/INACTIVE status keyed by claim_id. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimStatusService {

    private final ClaimStatusRepository repository;

    @Transactional(readOnly = true)
    public Map<String, String> getAllAsMap() {
        Map<String, String> out = new LinkedHashMap<>();
        repository.findAll().forEach(cs -> out.put(cs.getClaimId(), cs.getStatus().name()));
        return out;
    }

    @Transactional(readOnly = true)
    public ClaimStatusResponseDTO getOne(String claimId) {
        ClaimStatus cs = repository.findById(claimId)
                .orElseGet(() -> ClaimStatus.builder()
                        .claimId(claimId)
                        .status(ClaimStatusValue.ACTIVE) // default
                        .updatedAt(null)
                        .build());
        return new ClaimStatusResponseDTO(cs.getClaimId(), cs.getStatus(), cs.getUpdatedAt());
    }

    @Transactional
    public ClaimStatusResponseDTO upsert(String claimId, ClaimStatusValue status) {
        ClaimStatus entity = repository.findById(claimId)
                .orElseGet(() -> ClaimStatus.builder().claimId(claimId).build());
        entity.setStatus(status);
        ClaimStatus saved = repository.save(entity);
        log.info("Claim status upsert — claimId: {}, status: {}", claimId, status);
        return new ClaimStatusResponseDTO(saved.getClaimId(), saved.getStatus(), saved.getUpdatedAt());
    }
}
