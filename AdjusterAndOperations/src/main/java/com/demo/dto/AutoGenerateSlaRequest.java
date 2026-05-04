package com.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the auto-generate SLA violation endpoint.
 * Sent by data-ingestion-service immediately after a claim is ingested.
 *
 * <p>{@code claimRef} — string claim ID from ingestion service (e.g. "CLM-2026-AUTO-001").
 * A stable numeric claimId is derived from this via hashCode.</p>
 *
 * <p>{@code actualDays} — real elapsed days from the claim's incident/admission
 * date to today, extracted from the payload JSON. When absent, defaults to 31.</p>
 *
 * <p>{@code slaTargetDays} — SLA deadline in days (default 30).
 * {@code daysOverdue = actualDays - slaTargetDays} drives severity:
 * ≤2 → LOW · ≤5 → MEDIUM · ≤10 → HIGH · >10 → CRITICAL.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoGenerateSlaRequest {

    /** Original string claim reference from data-ingestion-service. */
    private String claimRef;

    /** Adjuster responsible for the claim. Defaults to 1 when null. */
    private Long adjusterId;

    /**
     * Actual elapsed days computed from the payload's incidentDate/admissionDate.
     * Null-safe: defaults to 31 inside the service when not provided.
     */
    private Integer actualDays;

    /**
     * SLA target in calendar days. Null-safe: defaults to 30 inside the service.
     */
    private Integer slaTargetDays;
}
