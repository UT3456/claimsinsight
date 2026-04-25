-- =============================================================
--  ClaimInsight360 — NotificationService seed data
--  Database: NotificationsDB
--  Covers: users, notifications, aging_records, risk_scores, sla_violations
-- =============================================================

-- ─── USERS (all roles; scheduler fan-out targets these) ───────────────────────
--  user_ids live in a 900000+ range so they never collide with gateway-owned
--  userIds (which auto-increment from 1). Gateway users are mirrored in via the
--  POST /api/notifications/users/sync endpoint and land at their actual ids.
INSERT IGNORE INTO users (user_id, name, email, role, is_active) VALUES
(900001, 'Alice Sharma',    'alice.sharma@claiminsight.com',    'ANALYST',   true),
(900002, 'Bob Martinez',    'bob.martinez@claiminsight.com',    'MANAGER',   true),
(900003, 'Carol Webb',      'carol.webb@claiminsight.com',      'FRAUD',     true),
(900004, 'David Chen',      'david.chen@claiminsight.com',      'ACTUARY',   true),
(900005, 'Eva Johnson',     'eva.johnson@claiminsight.com',     'EXECUTIVE', true),
(900006, 'Frank O\'Brien',  'frank.obrien@claiminsight.com',    'ADMIN',     true),
(900007, 'Grace Kim',       'grace.kim@claiminsight.com',       'ANALYST',   true),
(900008, 'Henry Patel',     'henry.patel@claiminsight.com',     'MANAGER',   true),
(900009, 'Irene Zhao',      'irene.zhao@claiminsight.com',      'FRAUD',     true),
(900010, 'James Wilson',    'james.wilson@claiminsight.com',    'ANALYST',   true);


-- ─── NOTIFICATIONS (direct seed — visible immediately in the UI) ──────────────
--  user_ids mirror the 900000+ range above. Real gateway-synced users never
--  see these unless they also have a synced user_id here (which they won't,
--  since the gateway assigns from 1).
-- AGING — for users 900001, 900002, 900007 (ANALYST / MANAGER roles)
INSERT IGNORE INTO notifications (user_id, title, message, category, reference_id, status, created_date, read_date) VALUES
(900001, 'Critically Aged Claim — CLM-1001',
     'Claim CLM-1001 has been OPEN for 125 days (bucket: 120+). Immediate action required.',
     'AGING', 'CLM-1001', 'UNREAD', NOW() - INTERVAL 1 DAY, NULL),

(900001, 'Critically Aged Claim — CLM-1002',
     'Claim CLM-1002 has been PENDING for 98 days (bucket: 91-120). Status is still PENDING. Escalation recommended.',
     'AGING', 'CLM-1002', 'UNREAD', NOW() - INTERVAL 2 DAY, NULL),

(900001, 'Critically Aged Claim — CLM-1003',
     'Claim CLM-1003 has been OPEN for 110 days (bucket: 91-120). Assignment reassignment needed.',
     'AGING', 'CLM-1003', 'READ',   NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY),

(900002, 'Aging Portfolio Alert — 12 Claims Critical',
     '12 claims in your portfolio have exceeded 90 days aging. Oldest: CLM-1001 at 125 days. Review the ReservesPage for full list.',
     'AGING', 'PORTFOLIO-Q1', 'UNREAD', NOW() - INTERVAL 3 HOUR, NULL),

(900007, 'Critically Aged Claim — CLM-1004',
     'Claim CLM-1004 has been OPEN for 115 days (bucket: 120+). Insurer SLA deadline passed.',
     'AGING', 'CLM-1004', 'UNREAD', NOW() - INTERVAL 6 HOUR, NULL),

-- RISK — for users 900003, 900009 (FRAUD roles)
(900003, 'High Risk Claim Detected — CLM-2001',
     'Claim CLM-2001 has a fraud risk score of 87 (CRITICAL). Duplicate claimant history found. Immediate review required.',
     'RISK', 'CLM-2001', 'UNREAD', NOW() - INTERVAL 30 MINUTE, NULL),

(900003, 'High Risk Claim Detected — CLM-2002',
     'Claim CLM-2002 has a fraud risk score of 74 (HIGH). Anomalous billing pattern detected from provider ID PRV-551.',
     'RISK', 'CLM-2002', 'UNREAD', NOW() - INTERVAL 2 HOUR, NULL),

(900003, 'High Risk Claim Detected — CLM-2003',
     'Claim CLM-2003 has a fraud risk score of 91 (CRITICAL). Claimant address matches 3 previously denied claims.',
     'RISK', 'CLM-2003', 'ACTIONED', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),

(900009, 'High Risk Claim Detected — CLM-2004',
     'Claim CLM-2004 has a fraud risk score of 68 (HIGH). Unusually high claim amount relative to policy premium.',
     'RISK', 'CLM-2004', 'UNREAD', NOW() - INTERVAL 1 HOUR, NULL),

(900009, 'High Risk Claim Detected — CLM-2005',
     'Claim CLM-2005 has a fraud risk score of 79 (HIGH). Third-party vendor is flagged in the watch-list database.',
     'RISK', 'CLM-2005', 'READ', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 20 HOUR),

-- DENIAL — for users 900001, 900004, 900007 (ANALYST / ACTUARY)
(900001, 'Denial Pattern Flagged — CLM-3001',
     'Claim CLM-3001 recorded denial code CO-4 (services not covered). Review denial reason and consider appeal eligibility.',
     'DENIAL', 'CLM-3001', 'UNREAD', NOW() - INTERVAL 4 HOUR, NULL),

(900001, 'Denial Pattern Flagged — CLM-3002',
     'Claim CLM-3002 recorded denial code PR-27 (prior authorization required). Appeal window closes in 14 days.',
     'DENIAL', 'CLM-3002', 'UNREAD', NOW() - INTERVAL 8 HOUR, NULL),

(900001, 'High-Value Leakage Flag — CLM-3003',
     'Claim CLM-3003 flagged for BILLING leakage. Estimated loss: $23,750. Requires investigation within 48 hours.',
     'DENIAL', 'CLM-3003', 'READ', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),

(900004, 'High-Value Leakage Flag — CLM-3004',
     'Claim CLM-3004 flagged for DUPLICATE_BILLING leakage. Estimated loss: $41,200. Reserve adequacy impacted.',
     'DENIAL', 'CLM-3004', 'UNREAD', NOW() - INTERVAL 5 HOUR, NULL),

(900007, 'Denial Pattern Flagged — CLM-3005',
     'Claim CLM-3005 recorded denial code CO-97 (payment adjusted because this procedure/service is not paid separately). Review for bundling issues.',
     'DENIAL', 'CLM-3005', 'UNREAD', NOW() - INTERVAL 12 HOUR, NULL),

(900007, 'High-Value Leakage Flag — CLM-3006',
     'Claim CLM-3006 flagged for UNDERPAYMENT leakage. Estimated loss: $15,900. Provider dispute pending.',
     'DENIAL', 'CLM-3006', 'DISMISSED', NOW() - INTERVAL 4 DAY, NULL),

-- COST / RESERVE — for users 900002, 900004, 900005, 900008 (MANAGER / ACTUARY / EXECUTIVE)
(900002, 'Large Reserve Alert — CLM-4001',
     'New reserve of $185,000 opened for claim CLM-4001. Reserve exceeds the $100K threshold — managerial review required.',
     'COST', 'CLM-4001', 'UNREAD', NOW() - INTERVAL 2 HOUR, NULL),

(900002, 'Large Reserve Alert — CLM-4002',
     'Reserve on claim CLM-4002 increased from $80,000 to $132,500. Crossed $100K threshold — actuarial sign-off needed.',
     'COST', 'CLM-4002', 'UNREAD', NOW() - INTERVAL 6 HOUR, NULL),

(900004, 'Critical Aging Alert — CLM-4003',
     'Claim CLM-4003 has been aging for 95 days and is in the BUCKET_90_PLUS category. Reserve adequacy review required.',
     'COST', 'CLM-4003', 'UNREAD', NOW() - INTERVAL 1 DAY, NULL),

(900005, 'Large Reserve Alert — CLM-4004',
     'New reserve of $220,000 opened for claim CLM-4004. Executive sign-off required for reserves exceeding $200K.',
     'COST', 'CLM-4004', 'UNREAD', NOW() - INTERVAL 30 MINUTE, NULL),

(900008, 'Large Reserve Alert — CLM-4005',
     'Reserve on claim CLM-4005 increased to $110,000. Month-over-month reserve increase: +38%. Review recommended.',
     'COST', 'CLM-4005', 'READ', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),

-- PERFORMANCE — for users 900002, 900005, 900008 (MANAGER / EXECUTIVE)
(900002, 'KPI Threshold Breach — CLM-5001',
     'Performance thresholds breached on claim CLM-5001: TAT 34d > 30d; Cycle time 65d > 60d. Review recommended.',
     'PERFORMANCE', 'CLM-5001', 'UNREAD', NOW() - INTERVAL 3 HOUR, NULL),

(900002, 'SLA Breach Threshold Exceeded — Adjuster #101',
     'Adjuster 101 has breached SLA on 5 claims this month. Threshold is 3. Please review workload and reassign if necessary.',
     'PERFORMANCE', 'ADJ-101', 'UNREAD', NOW() - INTERVAL 1 DAY, NULL),

(900002, 'SLA Breach Threshold Exceeded — Adjuster #102',
     'Adjuster 102 has breached SLA on 4 claims this month. Threshold is 3. Consider workload rebalancing.',
     'PERFORMANCE', 'ADJ-102', 'READ', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),

(900005, 'KPI Threshold Breach — CLM-5002',
     'Performance thresholds breached on claim CLM-5002: Loss ratio 1.35 > 1.0; Severity 8.5 ≥ 8. Executive review flagged.',
     'PERFORMANCE', 'CLM-5002', 'UNREAD', NOW() - INTERVAL 45 MINUTE, NULL),

(900008, 'SLA Breach Threshold Exceeded — Adjuster #103',
     'Adjuster 103 has breached SLA on 6 claims this month. Threshold is 3. Immediate reassignment recommended.',
     'PERFORMANCE', 'ADJ-103', 'UNREAD', NOW() - INTERVAL 5 HOUR, NULL),

-- SYSTEM — for a spread of users
(900001, 'System Maintenance Scheduled',
     'ClaimInsight360 will undergo scheduled maintenance on 2026-04-25 from 02:00–04:00 UTC. Reports generation will be temporarily unavailable.',
     'SYSTEM', 'MAINT-2026-04', 'READ', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY),

(900002, 'System Maintenance Scheduled',
     'ClaimInsight360 will undergo scheduled maintenance on 2026-04-25 from 02:00–04:00 UTC. Reports generation will be temporarily unavailable.',
     'SYSTEM', 'MAINT-2026-04', 'UNREAD', NOW() - INTERVAL 7 DAY, NULL),

(900003, 'New Fraud Detection Model Deployed',
     'Fraud risk scoring model v3.2 has been deployed. Minimum threshold updated from 60 to 61. Re-score results now available.',
     'SYSTEM', 'MODEL-V3.2', 'UNREAD', NOW() - INTERVAL 2 DAY, NULL),

(900004, 'Reserve Calculation Engine Updated',
     'Reserve calculation algorithm updated to include IBNR adjustments per Q1 actuarial guidelines. Historical reserves not affected.',
     'SYSTEM', 'RESERVE-Q1-2026', 'UNREAD', NOW() - INTERVAL 3 DAY, NULL),

(900005, 'Q1 2026 Analytics Reports Ready',
     'All Q1 2026 analytical reports are now available in the Reports section. Download PDF or CSV from the dashboard.',
     'SYSTEM', 'REPORT-Q1-2026', 'UNREAD', NOW() - INTERVAL 4 DAY, NULL),

(900006, 'User Access Audit Completed',
     'Monthly user access audit for March 2026 completed. 2 inactive users deactivated. Full audit log available.',
     'SYSTEM', 'AUDIT-MAR-2026', 'UNREAD', NOW() - INTERVAL 1 DAY, NULL);


-- ─── AGING RECORDS (triggers generateAgingAlerts scheduler) ──────────────────
INSERT IGNORE INTO aging_records (claim_id, aging_days, aging_bucket, as_of_date, status) VALUES
('CLM-1001', 125, '120+',   CURDATE(), 'OPEN'),
('CLM-1002',  98, '91-120', CURDATE(), 'PENDING'),
('CLM-1003', 110, '91-120', CURDATE(), 'OPEN'),
('CLM-1004', 115, '120+',   CURDATE(), 'OPEN'),
('CLM-1005', 132, '120+',   CURDATE(), 'PENDING'),
('CLM-1006',  92, '91-120', CURDATE(), 'OPEN'),
('CLM-1007', 105, '91-120', CURDATE(), 'PENDING'),
('CLM-1008',  45, '31-60',  CURDATE(), 'IN_REVIEW'),
('CLM-1009',  22, '0-30',   CURDATE(), 'OPEN'),
('CLM-1010',  67, '61-90',  CURDATE(), 'PENDING');


-- ─── RISK SCORES (triggers generateRiskAlerts scheduler) ─────────────────────
INSERT IGNORE INTO risk_scores (claim_id, score_value, risk_level, computed_date) VALUES
('CLM-2001', 87, 'CRITICAL', NOW() - INTERVAL 2 HOUR),
('CLM-2002', 74, 'HIGH',     NOW() - INTERVAL 4 HOUR),
('CLM-2003', 91, 'CRITICAL', NOW() - INTERVAL 6 HOUR),
('CLM-2004', 68, 'HIGH',     NOW() - INTERVAL 1 HOUR),
('CLM-2005', 79, 'HIGH',     NOW() - INTERVAL 3 HOUR),
('CLM-2006', 83, 'CRITICAL', NOW() - INTERVAL 5 HOUR),
('CLM-2007', 65, 'HIGH',     NOW() - INTERVAL 8 HOUR),
('CLM-2008', 95, 'CRITICAL', NOW() - INTERVAL 30 MINUTE),
('CLM-2009', 40, 'MEDIUM',   NOW() - INTERVAL 12 HOUR),
('CLM-2010', 25, 'LOW',      NOW() - INTERVAL 24 HOUR);


-- ─── SLA VIOLATIONS (triggers generatePerformanceAlerts scheduler) ───────────
INSERT IGNORE INTO sla_violations (claim_id, adjuster_id, violation_type, sla_target_days, actual_days, days_overdue, violation_date) VALUES
-- Adjuster 101: 5 violations this month
('CLM-5001', 101, 'Initial Review SLA Breach',     5,  9,  4, CURDATE() - INTERVAL 2 DAY),
('CLM-5003', 101, 'Decision SLA Breach',            7, 12,  5, CURDATE() - INTERVAL 4 DAY),
('CLM-5005', 101, 'Documentation SLA Breach',       3,  8,  5, CURDATE() - INTERVAL 6 DAY),
('CLM-5007', 101, 'Payment Processing SLA Breach',  5, 11,  6, CURDATE() - INTERVAL 8 DAY),
('CLM-5009', 101, 'Closure SLA Breach',             7, 14,  7, CURDATE() - INTERVAL 9 DAY),

-- Adjuster 102: 4 violations this month
('CLM-5002', 102, 'Initial Review SLA Breach',     5, 10,  5, CURDATE() - INTERVAL 1 DAY),
('CLM-5004', 102, 'Decision SLA Breach',            7, 15,  8, CURDATE() - INTERVAL 3 DAY),
('CLM-5006', 102, 'Documentation SLA Breach',       3,  6,  3, CURDATE() - INTERVAL 5 DAY),
('CLM-5008', 102, 'Payment Processing SLA Breach',  5, 13,  8, CURDATE() - INTERVAL 7 DAY),

-- Adjuster 103: 6 violations this month
('CLM-5010', 103, 'Initial Review SLA Breach',     5, 12,  7, CURDATE() - INTERVAL 1 DAY),
('CLM-5011', 103, 'Decision SLA Breach',            7, 18, 11, CURDATE() - INTERVAL 2 DAY),
('CLM-5012', 103, 'Documentation SLA Breach',       3,  9,  6, CURDATE() - INTERVAL 3 DAY),
('CLM-5013', 103, 'Payment Processing SLA Breach',  5, 14,  9, CURDATE() - INTERVAL 5 DAY),
('CLM-5014', 103, 'Closure SLA Breach',             7, 20, 13, CURDATE() - INTERVAL 7 DAY),
('CLM-5015', 103, 'Appeal Review SLA Breach',      10, 22, 12, CURDATE() - INTERVAL 10 DAY),

-- Adjuster 104: 2 violations (below threshold — should NOT trigger alert)
('CLM-5016', 104, 'Initial Review SLA Breach',     5,  7,  2, CURDATE() - INTERVAL 2 DAY),
('CLM-5017', 104, 'Decision SLA Breach',            7, 10,  3, CURDATE() - INTERVAL 8 DAY);
