package com.claim360.fraudrisk.enums;

/**
 * Lifecycle of a fraud investigation opened from a high-risk claim.
 *
 *   NEW          — just escalated by an analyst, awaiting SIU triage
 *   UNDER_REVIEW — assigned and actively being investigated
 *   CLOSED       — resolved (fraud confirmed, dismissed, or referred elsewhere)
 */
public enum InvestigationStatus {
    NEW,
    UNDER_REVIEW,
    CLOSED
}
