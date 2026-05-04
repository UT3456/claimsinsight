package com.claiminsight.ingestion.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors {@code AutoGenerateSlaRequest} from AdjusterAndOperations.
 * Sent to {@code POST /api/sla-violations/auto-generate} immediately
 * after a claim is successfully ingested into an ACTIVE data feed.
 *
 * <p>{@code actualDays} is computed from the payload's {@code incidentDate}
 * (or {@code admissionDate}) — the number of calendar days from that date
 * to today. When the field is absent from the payload, defaults to 31 so a
 * LOW-severity placeholder record is always created.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoGenerateSlaRequest {

    /** String claim reference, e.g. "CLM-2026-AUTO-001". */
    private String claimRef;

    /** Adjuster to assign. Defaults to 1 (unassigned) when null. */
    private Long adjusterId;

    /**
     * Real elapsed days from the claim's incident/admission date to today.
     * Drives the actual SLA breach severity calculation.
     * Falls back to 31 (LOW breach) when the payload has no date field.
     */
    private Integer actualDays;

    /**
     * SLA target in days for this claim type. Defaults to 30 when null.
     * daysOverdue = actualDays - slaTargetDays determines severity.
     */
    private Integer slaTargetDays;
}
