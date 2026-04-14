package com.claiminsight.gateway.identity.model;

/**
 * Roles available in the ClaimInsight360 platform.
 *
 * ROLE_CLAIMS_ANALYST   – Analyzes claim trends, denials, cost drivers.
 * ROLE_CLAIMS_MANAGER   – Oversees performance metrics, delays, workloads, cycle time.
 * ROLE_FRAUD_ANALYST    – Uses risk dashboards and red-flag indicators.
 * ROLE_ACTUARY          – Uses severity/frequency insights for pricing/reserving.
 * ROLE_OPERATIONS_EXEC  – Monitors KPIs, TAT, compliance SLAs.
 * ROLE_ADMIN            – Configures metrics, dashboards, data refresh, and roles.
 */
public enum Role {
    ROLE_CLAIMS_ANALYST,
    ROLE_CLAIMS_MANAGER,
    ROLE_FRAUD_ANALYST,
    ROLE_ACTUARY,
    ROLE_OPERATIONS_EXEC,
    ROLE_ADMIN
}
