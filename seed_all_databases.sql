-- ====================================================================
-- ClaimInsight360 - Comprehensive Seed Data Script
-- Run this directly against MySQL:
--   mysql -u root -proot < seed_all_databases.sql
--
-- Covers all 7 business databases.
-- Uses INSERT IGNORE - safe to re-run without duplicating rows.
-- ====================================================================

-- ====================================================================
-- 1.  ADJUSTERPERFORMANCEDB
--     Tables: claim, adjuster_performance, sla_violation
-- ====================================================================
USE AdjusterPerformanceDB;

-- ── claim (20 rows) ──────────────────────────────────────────────────
INSERT IGNORE INTO claim (claim_id, policy_id, customer_id, claim_type, claim_status, reported_date, settled_date, amount) VALUES
(1,  101, 201, 'Auto',      'Closed',             '2026-01-05', '2026-02-10', 8500.00),
(2,  102, 202, 'Auto',      'Under Review',       '2026-01-08', NULL,         4200.00),
(3,  103, 203, 'Auto',      'Total Loss',         '2026-01-12', NULL,         35000.00),
(4,  104, 204, 'Auto',      'Approved',           '2026-01-15', '2026-02-18', 12000.00),
(5,  105, 205, 'Auto',      'Closed',             '2026-01-18', '2026-01-20', 1800.00),
(6,  106, 206, 'Property',  'Open',               '2026-01-20', NULL,         22000.00),
(7,  107, 207, 'Property',  'Under Investigation','2026-01-22', NULL,         45000.00),
(8,  108, 208, 'Property',  'Approved',           '2026-01-25', '2026-02-28', 9500.00),
(9,  109, 209, 'Medical',   'Open',               '2026-02-01', NULL,         18500.00),
(10, 110, 210, 'Medical',   'Approved',           '2026-02-05', '2026-03-05', 14200.00),
(11, 111, 211, 'Medical',   'Under Review',       '2026-02-10', NULL,         32000.00),
(12, 112, 212, 'Auto',      'Closed',             '2026-02-14', '2026-02-16', 650.00),
(13, 113, 213, 'Auto',      'Approved',           '2026-02-18', '2026-02-25', 3800.00),
(14, 114, 214, 'Property',  'Open',               '2026-03-01', NULL,         7800.00),
(15, 115, 215, 'Medical',   'Approved',           '2026-03-05', '2026-03-20', 2400.00),
(16, 116, 216, 'Liability', 'Under Review',       '2026-01-16', NULL,         8200.00),
(17, 117, 217, 'Liability', 'Under Investigation','2026-02-03', NULL,         77000.00),
(18, 118, 218, 'Auto',      'Under Review',       '2026-02-12', NULL,         18500.00),
(19, 119, 219, 'Liability', 'Open',               '2026-02-20', NULL,         52000.00),
(20, 120, 220, 'Auto',      'Approved',           '2026-03-02', '2026-03-12', 2800.00);

-- ── adjuster_performance (15 rows - 5 adjusters × 3 periods) ─────────
INSERT IGNORE INTO adjuster_performance
  (perf_id, adjuster_id, claims_handled, total_days_taken, avg_tat, quality_score, sla_met_count, sla_breached_count, denied_claims_count, error_rate, period)
VALUES
-- Adjuster 1: Mike Torres - strong volume, occasional SLA breach
(1,  1, 38, 342, 9.0,  8.7, 34, 4, 3, 0.05, 'Q1-2026'),
(2,  1, 35, 350, 10.0, 8.5, 30, 5, 4, 0.06, 'Q4-2025'),
(3,  1, 30, 330, 11.0, 8.3, 25, 5, 3, 0.07, 'Q3-2025'),
-- Adjuster 2: Sarah Lee - fastest TAT, high quality
(4,  2, 42, 294, 7.0,  9.2, 40, 2, 2, 0.03, 'Q1-2026'),
(5,  2, 40, 320, 8.0,  9.0, 37, 3, 2, 0.03, 'Q4-2025'),
(6,  2, 36, 324, 9.0,  8.9, 33, 3, 2, 0.04, 'Q3-2025'),
-- Adjuster 3: James Park - handles complex high-value claims
(7,  3, 22, 440, 20.0, 8.9, 18, 4, 2, 0.04, 'Q1-2026'),
(8,  3, 20, 460, 23.0, 8.6, 16, 4, 2, 0.05, 'Q4-2025'),
(9,  3, 18, 468, 26.0, 8.4, 14, 4, 2, 0.06, 'Q3-2025'),
-- Adjuster 4: Lisa Chen - high quality, medium volume
(10, 4, 33, 264, 8.0,  9.5, 31, 2, 1, 0.02, 'Q1-2026'),
(11, 4, 30, 270, 9.0,  9.3, 28, 2, 1, 0.02, 'Q4-2025'),
(12, 4, 27, 270, 10.0, 9.1, 25, 2, 1, 0.03, 'Q3-2025'),
-- Adjuster 5: Tom Wilson - specialist in minor/quick claims, high volume
(13, 5, 55, 275, 5.0,  8.2, 52, 3, 5, 0.08, 'Q1-2026'),
(14, 5, 50, 300, 6.0,  8.0, 46, 4, 5, 0.08, 'Q4-2025'),
(15, 5, 45, 315, 7.0,  7.9, 41, 4, 4, 0.09, 'Q3-2025');

-- ── sla_violation (22 rows) ──────────────────────────────────────────
INSERT IGNORE INTO sla_violation
  (violation_id, claim_id, adjuster_id, violation_type, sla_target_days, actual_days, violation_date)
VALUES
-- RESPONSE_DELAY: target 2 days, breached
(1,  3,  3, 'RESPONSE_DELAY',     2,  5,  '2026-01-14'),
(2,  6,  1, 'RESPONSE_DELAY',     2,  4,  '2026-01-22'),
(3,  7,  1, 'RESPONSE_DELAY',     2,  6,  '2026-01-24'),
(4,  9,  4, 'RESPONSE_DELAY',     2,  4,  '2026-02-03'),
(5,  17, 3, 'RESPONSE_DELAY',     2,  7,  '2026-02-05'),
(6,  19, 3, 'RESPONSE_DELAY',     2,  9,  '2026-02-22'),
-- INSPECTION_DELAY: target 7 days
(7,  3,  3, 'INSPECTION_DELAY',   7,  14, '2026-01-19'),
(8,  6,  1, 'INSPECTION_DELAY',   7,  12, '2026-01-27'),
(9,  7,  1, 'INSPECTION_DELAY',   7,  18, '2026-01-29'),
(10, 11, 4, 'INSPECTION_DELAY',   7,  11, '2026-02-17'),
(11, 17, 3, 'INSPECTION_DELAY',   7,  22, '2026-02-10'),
-- SETTLEMENT_BREACH: target 30 days
(12, 3,  3, 'SETTLEMENT_BREACH',  30, 45, '2026-02-11'),
(13, 7,  1, 'SETTLEMENT_BREACH',  30, 62, '2026-02-21'),
(14, 9,  4, 'SETTLEMENT_BREACH',  30, 38, '2026-03-03'),
(15, 17, 3, 'SETTLEMENT_BREACH',  30, 90, '2026-05-04'),
(16, 19, 3, 'SETTLEMENT_BREACH',  30, 75, '2026-05-21'),
-- DOCUMENTATION_DELAY: target 5 days
(17, 2,  2, 'DOCUMENTATION_DELAY', 5, 9,  '2026-01-13'),
(18, 9,  4, 'DOCUMENTATION_DELAY', 5, 8,  '2026-02-06'),
(19, 11, 4, 'DOCUMENTATION_DELAY', 5, 12, '2026-02-15'),
(20, 14, 1, 'DOCUMENTATION_DELAY', 5, 7,  '2026-03-06'),
(21, 18, 2, 'DOCUMENTATION_DELAY', 5, 8,  '2026-02-17'),
(22, 16, 5, 'DOCUMENTATION_DELAY', 5, 6,  '2026-01-21');


-- ====================================================================
-- 2.  CLAIMS_COST_RESERVE_DB
--     Tables: claim_cost, claim_reserve, aging_record
-- ====================================================================
USE claims_cost_reserve_db;

-- ── claim_cost (30 rows) ─────────────────────────────────────────────
INSERT IGNORE INTO claim_cost (cost_id, claim_id, cost_type, amount, cost_date) VALUES
(1,  'CLM-2026-AUTO-001', 'REPAIR',     8200.00,  '2026-01-10'),
(2,  'CLM-2026-AUTO-001', 'LEGAL',      300.00,   '2026-01-15'),
(3,  'CLM-2026-AUTO-002', 'REPAIR',     4000.00,  '2026-01-12'),
(4,  'CLM-2026-AUTO-003', 'REPAIR',     38000.00, '2026-01-15'),
(5,  'CLM-2026-AUTO-003', 'LEGAL',      1200.00,  '2026-01-20'),
(6,  'CLM-2026-AUTO-004', 'REPAIR',     11500.00, '2026-01-20'),
(7,  'CLM-2026-AUTO-005', 'REPAIR',     1750.00,  '2026-01-20'),
(8,  'CLM-2026-PROP-001', 'REPAIR',     18000.00, '2026-01-25'),
(9,  'CLM-2026-PROP-001', 'MEDICAL',    2500.00,  '2026-01-28'),
(10, 'CLM-2026-PROP-002', 'REPAIR',     40000.00, '2026-01-28'),
(11, 'CLM-2026-PROP-002', 'LEGAL',      5000.00,  '2026-02-05'),
(12, 'CLM-2026-PROP-003', 'REPAIR',     9500.00,  '2026-01-30'),
(13, 'CLM-2026-MED-001',  'MEDICAL',    18500.00, '2026-02-03'),
(14, 'CLM-2026-MED-002',  'MEDICAL',    14200.00, '2026-02-08'),
(15, 'CLM-2026-MED-003',  'MEDICAL',    32000.00, '2026-02-15'),
(16, 'CLM-2026-MED-003',  'LEGAL',      3500.00,  '2026-02-20'),
(17, 'CLM-2026-FC-001',   'REPAIR',     2100.00,  '2026-01-10'),
(18, 'CLM-2026-FC-002',   'MEDICAL',    13800.00, '2026-01-18'),
(19, 'CLM-2026-FC-002',   'LEGAL',      1200.00,  '2026-01-22'),
(20, 'CLM-2026-FC-003',   'MEDICAL',    5200.00,  '2026-01-22'),
(21, 'CLM-2026-FC-003',   'LEGAL',      3000.00,  '2026-01-28'),
(22, 'CLM-2026-FC-004',   'REPAIR',     28000.00, '2026-01-25'),
(23, 'CLM-2026-FC-006',   'REPAIR',     62000.00, '2026-02-08'),
(24, 'CLM-2026-FC-006',   'LEGAL',      15000.00, '2026-02-15'),
(25, 'CLM-2026-FC-009',   'MEDICAL',    22000.00, '2026-02-25'),
(26, 'CLM-2026-FC-009',   'LEGAL',      12000.00, '2026-03-01'),
(27, 'CLM-ENT-001',       'LEGAL',      75000.00, '2026-02-22'),
(28, 'CLM-ENT-001',       'SETTLEMENT', 250000.00,'2026-03-01'),
(29, 'CLM-ENT-002',       'MEDICAL',    22000.00, '2026-02-25'),
(30, 'CLM-ENT-002',       'LEGAL',      30000.00, '2026-03-01');

-- ── claim_reserve (16 rows) ──────────────────────────────────────────
INSERT IGNORE INTO claim_reserve (reserve_id, claim_id, reserve_amount, updated_date) VALUES
(1,  'CLM-2026-AUTO-001', 9000.00,   '2026-01-06'),
(2,  'CLM-2026-AUTO-002', 5000.00,   '2026-01-09'),
(3,  'CLM-2026-AUTO-003', 40000.00,  '2026-01-13'),
(4,  'CLM-2026-AUTO-004', 13000.00,  '2026-01-16'),
(5,  'CLM-2026-PROP-001', 25000.00,  '2026-01-21'),
(6,  'CLM-2026-PROP-002', 55000.00,  '2026-01-23'),
(7,  'CLM-2026-PROP-003', 10000.00,  '2026-01-26'),
(8,  'CLM-2026-MED-001',  20000.00,  '2026-02-02'),
(9,  'CLM-2026-MED-002',  15000.00,  '2026-02-06'),
(10, 'CLM-2026-MED-003',  38000.00,  '2026-02-11'),
(11, 'CLM-2026-FC-004',   32000.00,  '2026-01-22'),
(12, 'CLM-2026-FC-006',   100000.00, '2026-02-05'),
(13, 'CLM-2026-FC-009',   80000.00,  '2026-02-22'),
(14, 'CLM-ENT-001',       400000.00, '2026-02-21'),
(15, 'CLM-ENT-002',       150000.00, '2026-02-23'),
(16, 'CLM-ENT-003',       500000.00, '2026-03-02');

-- ── aging_record (20 rows) ──────────────────────────────────────────
INSERT IGNORE INTO aging_record (aging_id, claim_id, aging_days, aging_bucket) VALUES
(1,  'CLM-2026-AUTO-001', 15,  'BUCKET_0_30'),
(2,  'CLM-2026-AUTO-002', 28,  'BUCKET_0_30'),
(3,  'CLM-2026-AUTO-003', 45,  'BUCKET_31_60'),
(4,  'CLM-2026-AUTO-004', 8,   'BUCKET_0_30'),
(5,  'CLM-2026-PROP-001', 55,  'BUCKET_31_60'),
(6,  'CLM-2026-PROP-002', 78,  'BUCKET_61_90'),
(7,  'CLM-2026-PROP-003', 12,  'BUCKET_0_30'),
(8,  'CLM-2026-MED-001',  35,  'BUCKET_31_60'),
(9,  'CLM-2026-MED-002',  20,  'BUCKET_0_30'),
(10, 'CLM-2026-MED-003',  62,  'BUCKET_61_90'),
(11, 'CLM-2026-FC-001',   5,   'BUCKET_0_30'),
(12, 'CLM-2026-FC-002',   30,  'BUCKET_0_30'),
(13, 'CLM-2026-FC-003',   42,  'BUCKET_31_60'),
(14, 'CLM-2026-FC-004',   25,  'BUCKET_0_30'),
(15, 'CLM-2026-FC-006',   95,  'BUCKET_90_PLUS'),
(16, 'CLM-2026-FC-007',   3,   'BUCKET_0_30'),
(17, 'CLM-2026-FC-009',   110, 'BUCKET_90_PLUS'),
(18, 'CLM-ENT-001',       130, 'BUCKET_90_PLUS'),
(19, 'CLM-ENT-002',       105, 'BUCKET_90_PLUS'),
(20, 'CLM-ENT-003',       120, 'BUCKET_90_PLUS');


-- ====================================================================
-- 3.  DENIAL_LEAKAGE
--     Tables: denial_pattern, leakage_flag
-- ====================================================================
USE denial_leakage;

-- ── denial_pattern (20 rows) ─────────────────────────────────────────
INSERT IGNORE INTO denial_pattern (pattern_id, claim_id, denial_code, reason, occurrence_date) VALUES
(1,  'CLM-2026-AUTO-002', 'D001', 'Policy lapsed at time of incident - no coverage in force',          '2026-01-20'),
(2,  'CLM-2026-AUTO-003', 'D002', 'Damage pre-existing - not caused by reported incident',             '2026-01-25'),
(3,  'CLM-2026-PROP-001', 'D003', 'Exclusion applies - gradual water damage not a sudden event',       '2026-02-01'),
(4,  'CLM-2026-PROP-002', 'D004', 'Arson suspicion - fire origin under investigation',                 '2026-02-05'),
(5,  'CLM-2026-MED-001',  'D005', 'Treatment not medically necessary per clinical review',             '2026-02-10'),
(6,  'CLM-2026-MED-003',  'D006', 'Procedure not covered under policy schedule of benefits',           '2026-02-20'),
(7,  'CLM-2026-FC-003',   'D007', 'Liability not established - contributory negligence applies',       '2026-01-25'),
(8,  'CLM-2026-FC-006',   'D008', 'Product defect claim exceeds single-occurrence policy limit',       '2026-02-15'),
(9,  'CLM-2026-FC-009',   'D009', 'Employer liability exclusion - employee assumed risk',              '2026-03-05'),
(10, 'CLM-ENT-001',       'D010', 'Cyber incident not covered under property policy',                  '2026-02-28'),
(11, 'CLM-2026-AUTO-006', 'D001', 'Policy excess exceeds claim amount - no net payment due',          '2026-02-20'),
(12, 'CLM-2026-FC-002',   'D011', 'Injury claim submitted after 90-day limitation period',             '2026-01-28'),
(13, 'CLM-2026-PROP-003', 'D012', 'Items claimed not listed on home contents schedule',               '2026-02-08'),
(14, 'CLM-2026-MED-002',  'D005', 'Second surgical opinion conflicts with treating physician',         '2026-02-18'),
(15, 'CLM-ENT-002',       'D013', 'Scaffold safety regulation non-compliance voids employer cover',    '2026-03-10'),
(16, 'CLM-2026-AUTO-004', 'D002', 'Vehicle modification undisclosed - cover voided',                  '2026-01-30'),
(17, 'CLM-2026-FC-004',   'D014', 'Flood zone exclusion applied - policy does not cover floodwater',  '2026-02-01'),
(18, 'CLM-2026-FC-008',   'D015', 'Third party liability disputed - independent report pending',       '2026-02-18'),
(19, 'CLM-2026-AUTO-007', 'D016', 'Vandalism claim - CCTV evidence contradicts claimant account',     '2026-02-24'),
(20, 'CLM-ENT-003',       'D017', 'Cyber event was nation-state attack - war exclusion applies',       '2026-03-12');

-- ── leakage_flag (18 rows) ───────────────────────────────────────────
INSERT IGNORE INTO leakage_flag (leakage_id, claim_id, leakage_type, estimated_loss, identified_date) VALUES
(1,  'CLM-2026-AUTO-001', 'Overpayment',  1200.00,  '2026-02-12'),
(2,  'CLM-2026-AUTO-002', 'Delay',        500.00,   '2026-01-28'),
(3,  'CLM-2026-AUTO-003', 'Overpayment',  3500.00,  '2026-02-01'),
(4,  'CLM-2026-PROP-001', 'Error',        2800.00,  '2026-02-05'),
(5,  'CLM-2026-PROP-002', 'Overpayment',  7500.00,  '2026-02-12'),
(6,  'CLM-2026-PROP-003', 'Error',        900.00,   '2026-02-02'),
(7,  'CLM-2026-MED-001',  'Overpayment',  4200.00,  '2026-02-15'),
(8,  'CLM-2026-MED-002',  'Delay',        800.00,   '2026-02-20'),
(9,  'CLM-2026-MED-003',  'Overpayment',  6800.00,  '2026-02-28'),
(10, 'CLM-2026-FC-002',   'Delay',        1500.00,  '2026-02-01'),
(11, 'CLM-2026-FC-003',   'Error',        1200.00,  '2026-02-05'),
(12, 'CLM-2026-FC-006',   'Overpayment',  18000.00, '2026-02-20'),
(13, 'CLM-2026-FC-009',   'Overpayment',  12000.00, '2026-03-08'),
(14, 'CLM-ENT-001',       'Overpayment',  45000.00, '2026-03-05'),
(15, 'CLM-ENT-002',       'Delay',        9500.00,  '2026-03-12'),
(16, 'CLM-ENT-003',       'Error',        25000.00, '2026-03-15'),
(17, 'CLM-2026-AUTO-004', 'Error',        650.00,   '2026-02-22'),
(18, 'CLM-2026-FC-008',   'Overpayment',  3200.00,  '2026-02-25');


-- ====================================================================
-- 4.  FRAUD_RISK_DB
--     Tables: risk_score, risk_indicator
-- ====================================================================
USE fraud_risk_db;

-- ── risk_score (25 rows - mix of low/medium/high/critical risk) ──────
INSERT IGNORE INTO risk_score (score_id, claim_id, score_value, computed_date) VALUES
(1,  'CLM-2026-AUTO-001', 22.5,  '2026-01-07'),
(2,  'CLM-2026-AUTO-002', 18.0,  '2026-01-09'),
(3,  'CLM-2026-AUTO-003', 58.0,  '2026-01-13'),
(4,  'CLM-2026-AUTO-004', 31.5,  '2026-01-16'),
(5,  'CLM-2026-AUTO-005', 8.0,   '2026-01-19'),
(6,  'CLM-2026-PROP-001', 44.0,  '2026-01-21'),
(7,  'CLM-2026-PROP-002', 88.5,  '2026-01-23'),   -- HIGH RISK fire investigation
(8,  'CLM-2026-PROP-003', 35.0,  '2026-01-26'),
(9,  'CLM-2026-MED-001',  52.0,  '2026-02-02'),
(10, 'CLM-2026-MED-002',  27.0,  '2026-02-06'),
(11, 'CLM-2026-MED-003',  61.0,  '2026-02-11'),
(12, 'CLM-2026-FC-001',   12.0,  '2026-01-08'),
(13, 'CLM-2026-FC-002',   46.0,  '2026-01-11'),
(14, 'CLM-2026-FC-003',   69.0,  '2026-01-17'),
(15, 'CLM-2026-FC-004',   77.5,  '2026-01-21'),   -- HIGH RISK flood/total loss
(16, 'CLM-2026-FC-005',   5.5,   '2026-01-29'),
(17, 'CLM-2026-FC-006',   95.0,  '2026-02-04'),   -- CRITICAL product liability
(18, 'CLM-2026-FC-007',   9.0,   '2026-02-08'),
(19, 'CLM-2026-FC-008',   55.0,  '2026-02-13'),
(20, 'CLM-2026-FC-009',   82.0,  '2026-02-21'),   -- HIGH RISK employer liability
(21, 'CLM-2026-FC-010',   23.0,  '2026-03-03'),
(22, 'CLM-ENT-001',       91.0,  '2026-02-21'),   -- CRITICAL product class action
(23, 'CLM-ENT-002',       78.5,  '2026-02-22'),   -- HIGH RISK employer liability
(24, 'CLM-ENT-003',       97.5,  '2026-03-01'),   -- CRITICAL cyber incident
(25, 'CLM-2026-PROP-004', 38.0,  '2026-03-02');

-- ── risk_indicator (22 rows) ─────────────────────────────────────────
INSERT IGNORE INTO risk_indicator (indicator_id, claim_id, indicator_type, severity, triggered_date) VALUES
(1,  'CLM-2026-PROP-002', 'HighCost',        'HIGH',     '2026-01-23'),
(2,  'CLM-2026-PROP-002', 'UnusualTiming',   'MEDIUM',   '2026-01-23'),
(3,  'CLM-2026-FC-004',   'HighCost',        'HIGH',     '2026-01-21'),
(4,  'CLM-2026-FC-004',   'Pattern',         'MEDIUM',   '2026-01-21'),
(5,  'CLM-2026-FC-006',   'HighCost',        'CRITICAL', '2026-02-04'),
(6,  'CLM-2026-FC-006',   'Pattern',         'CRITICAL', '2026-02-04'),
(7,  'CLM-2026-FC-006',   'UnusualTiming',   'HIGH',     '2026-02-05'),
(8,  'CLM-2026-FC-009',   'HighCost',        'HIGH',     '2026-02-21'),
(9,  'CLM-2026-FC-009',   'Pattern',         'HIGH',     '2026-02-22'),
(10, 'CLM-ENT-001',       'HighCost',        'CRITICAL', '2026-02-21'),
(11, 'CLM-ENT-001',       'Pattern',         'CRITICAL', '2026-02-21'),
(12, 'CLM-ENT-001',       'UnusualTiming',   'HIGH',     '2026-02-22'),
(13, 'CLM-ENT-002',       'HighCost',        'HIGH',     '2026-02-22'),
(14, 'CLM-ENT-002',       'UnusualTiming',   'MEDIUM',   '2026-02-23'),
(15, 'CLM-ENT-003',       'HighCost',        'CRITICAL', '2026-03-01'),
(16, 'CLM-ENT-003',       'UnusualTiming',   'CRITICAL', '2026-03-01'),
(17, 'CLM-ENT-003',       'Pattern',         'CRITICAL', '2026-03-02'),
(18, 'CLM-2026-MED-001',  'UnusualTiming',   'MEDIUM',   '2026-02-02'),
(19, 'CLM-2026-MED-003',  'HighCost',        'HIGH',     '2026-02-11'),
(20, 'CLM-2026-AUTO-003', 'Pattern',         'MEDIUM',   '2026-01-13'),
(21, 'CLM-2026-FC-003',   'UnusualTiming',   'MEDIUM',   '2026-01-17'),
(22, 'CLM-2026-FC-002',   'Pattern',         'LOW',      '2026-01-11');


-- ====================================================================
-- 5.  CLAIMS_ANALYTICS_REPORT_DB
--     Tables: analytics_report
-- ====================================================================
USE claims_analytics_report_db;

-- ── analytics_report (16 rows - all 4 scope types) ──────────────────
INSERT IGNORE INTO analytics_report
  (report_id, scope, scope_value, metrics, generated_date, generated_by, report_data)
VALUES
-- PRODUCT scope
(1,  'PRODUCT', 'Auto Insurance',
     'TAT,LossRatio,ClaimVolume,DenialRate,AvgSettlement',
     '2026-03-01', 'admin_alice',
     '{"claimVolume":28,"avgTAT":8.4,"lossRatio":0.71,"denialRate":0.12,"avgSettlement":9850.00,"topDenialCode":"D002","trend":"stable"}'),
(2,  'PRODUCT', 'Property Insurance',
     'TAT,LossRatio,ClaimVolume,DenialRate,AvgSettlement',
     '2026-03-01', 'admin_alice',
     '{"claimVolume":12,"avgTAT":24.5,"lossRatio":0.89,"denialRate":0.18,"avgSettlement":28000.00,"topDenialCode":"D003","trend":"increasing"}'),
(3,  'PRODUCT', 'Medical Insurance',
     'TAT,LossRatio,ClaimVolume,DenialRate,AvgSettlement',
     '2026-03-01', 'admin_alice',
     '{"claimVolume":8,"avgTAT":19.2,"lossRatio":0.84,"denialRate":0.22,"avgSettlement":16775.00,"topDenialCode":"D005","trend":"stable"}'),
(4,  'PRODUCT', 'Liability Insurance',
     'TAT,LossRatio,ClaimVolume,DenialRate,AvgSettlement',
     '2026-03-02', 'admin_alice',
     '{"claimVolume":6,"avgTAT":42.0,"lossRatio":1.25,"denialRate":0.28,"avgSettlement":45667.00,"topDenialCode":"D007","trend":"increasing","flagged":true}'),
-- REGION scope
(5,  'REGION', 'London',
     'ClaimVolume,LossRatio,AvgTAT,TopClaimType',
     '2026-03-05', 'admin_alice',
     '{"claimVolume":22,"lossRatio":0.82,"avgTAT":14.3,"topClaimType":"Auto","highRiskCount":5}'),
(6,  'REGION', 'North West',
     'ClaimVolume,LossRatio,AvgTAT,TopClaimType',
     '2026-03-05', 'admin_alice',
     '{"claimVolume":15,"lossRatio":0.76,"avgTAT":11.8,"topClaimType":"Property","highRiskCount":3}'),
(7,  'REGION', 'Midlands',
     'ClaimVolume,LossRatio,AvgTAT,TopClaimType',
     '2026-03-05', 'admin_alice',
     '{"claimVolume":12,"lossRatio":0.91,"avgTAT":18.2,"topClaimType":"Liability","highRiskCount":6,"flagged":true}'),
(8,  'REGION', 'Scotland',
     'ClaimVolume,LossRatio,AvgTAT,TopClaimType',
     '2026-03-05', 'admin_alice',
     '{"claimVolume":8,"lossRatio":0.68,"avgTAT":9.5,"topClaimType":"Auto","highRiskCount":1}'),
-- CLAIM_TYPE scope
(9,  'CLAIM_TYPE', 'Total Loss',
     'ClaimVolume,AvgAmount,LossRatio,AvgTAT',
     '2026-03-08', 'admin_alice',
     '{"claimVolume":4,"avgAmount":35500.00,"lossRatio":1.05,"avgTAT":22.5,"subrogationRecovery":8500.00}'),
(10, 'CLAIM_TYPE', 'Theft',
     'ClaimVolume,AvgAmount,RecoveryRate,AvgTAT',
     '2026-03-08', 'admin_alice',
     '{"claimVolume":6,"avgAmount":4800.00,"recoveryRate":0.15,"avgTAT":6.2}'),
(11, 'CLAIM_TYPE', 'Water Damage',
     'ClaimVolume,AvgAmount,LossRatio,DenialRate',
     '2026-03-08', 'admin_alice',
     '{"claimVolume":5,"avgAmount":19600.00,"lossRatio":0.94,"denialRate":0.20,"exclusionApplied":2}'),
(12, 'CLAIM_TYPE', 'Liability - Employer',
     'ClaimVolume,AvgAmount,LegalCost,LossRatio',
     '2026-03-08', 'admin_alice',
     '{"claimVolume":3,"avgAmount":44333.00,"avgLegalCost":18000.00,"lossRatio":1.32,"flagged":true}'),
-- PERIOD scope
(13, 'PERIOD', 'Q1-2026',
     'ClaimVolume,GrossPremium,TotalClaims,LossRatio,SLABreachRate,FraudFlag',
     '2026-04-01', 'admin_alice',
     '{"claimVolume":54,"grossPremium":425000.00,"totalClaimsPaid":312000.00,"lossRatio":0.734,"slaBreachRate":0.18,"fraudFlagRate":0.12,"newPolicies":38,"renewals":61,"cancellations":7}'),
(14, 'PERIOD', 'Q4-2025',
     'ClaimVolume,GrossPremium,TotalClaims,LossRatio,SLABreachRate',
     '2026-01-05', 'admin_alice',
     '{"claimVolume":48,"grossPremium":398000.00,"totalClaimsPaid":285000.00,"lossRatio":0.716,"slaBreachRate":0.15,"fraudFlagRate":0.09}'),
(15, 'PERIOD', 'Q3-2025',
     'ClaimVolume,GrossPremium,TotalClaims,LossRatio',
     '2025-10-03', 'admin_alice',
     '{"claimVolume":41,"grossPremium":375000.00,"totalClaimsPaid":258000.00,"lossRatio":0.688,"slaBreachRate":0.13}'),
(16, 'PERIOD', 'H1-2026-Forecast',
     'ClaimVolume,ProjectedLoss,RiskExposure',
     '2026-04-01', 'admin_alice',
     '{"projectedClaimVolume":110,"projectedLoss":680000.00,"riskExposure":"ELEVATED","driverNote":"Employer liability and cyber claims trending upward","confidence":0.82}');


-- ====================================================================
-- 6.  NOTIFICATIONSDB
--     Tables: users (mock), notifications, sla_violations (mock),
--             risk_scores (mock), aging_records (mock)
-- ====================================================================
USE NotificationsDB;

-- ── users mock (6 rows) ──────────────────────────────────────────────
INSERT IGNORE INTO users (user_id, name, email, role, is_active) VALUES
(1, 'Admin Alice',    'admin@claiminsight360.com',    'ADMIN',     true),
(2, 'Bob Analyst',    'bob.analyst@claiminsight360.com',  'ANALYST',   true),
(3, 'Carol Manager',  'carol.mgr@claiminsight360.com',    'MANAGER',   true),
(4, 'Dan Fraud',      'dan.fraud@claiminsight360.com',    'FRAUD',     true),
(5, 'Eva Actuary',    'eva.actuary@claiminsight360.com',  'ACTUARY',   true),
(6, 'Frank Exec',     'frank.exec@claiminsight360.com',   'EXECUTIVE', true);

-- ── notifications (30 rows - mix of categories, statuses) ────────────
INSERT IGNORE INTO notifications
  (notification_id, user_id, title, message, category, reference_id, status, created_date, read_date)
VALUES
-- RISK alerts
(1,  1, 'Critical Fraud Risk - CLM-2026-PROP-002',
     'Claim CLM-2026-PROP-002 has reached a fraud risk score of 88.5. Fire origin remains under investigation. Immediate escalation recommended.',
     'RISK', 'CLM-2026-PROP-002', 'UNREAD', '2026-01-23 11:30:00', NULL),
(2,  1, 'Critical Fraud Risk - CLM-2026-FC-006',
     'Product liability claim CLM-2026-FC-006 has been flagged CRITICAL (score 95.0). Potential class action exposure detected.',
     'RISK', 'CLM-2026-FC-006', 'UNREAD', '2026-02-04 09:15:00', NULL),
(3,  4, 'High Risk Flag - CLM-2026-FC-009',
     'Employer liability claim CLM-2026-FC-009 scored 82.0. HighCost and Pattern indicators have been triggered.',
     'RISK', 'CLM-2026-FC-009', 'READ', '2026-02-21 10:00:00', '2026-02-21 14:30:00'),
(4,  4, 'Critical Risk Score - CLM-ENT-003',
     'Cyber incident CLM-ENT-003 has reached the maximum risk score of 97.5. Three simultaneous risk indicators active.',
     'RISK', 'CLM-ENT-003', 'UNREAD', '2026-03-01 08:45:00', NULL),
(5,  4, 'High Risk - CLM-ENT-001',
     'Product class action claim CLM-ENT-001 scored 91.0. Legal and Pattern indicators triggered.',
     'RISK', 'CLM-ENT-001', 'ACTIONED', '2026-02-21 09:00:00', '2026-02-22 08:00:00'),
-- SLA VIOLATION alerts
(6,  1, 'Critical SLA Violation - Adjuster 3',
     'Adjuster 3 (James Park) has a CRITICAL SLA violation (SETTLEMENT_BREACH) with 60 days overdue for claim CLM-ENT-001.',
     'PERFORMANCE', 'CLM-ENT-001', 'UNREAD', '2026-05-05 08:00:00', NULL),
(7,  3, 'SLA Breach Alert - Settlement Delay',
     'Claim CLM-2026-FC-009 has exceeded the 30-day settlement SLA by 45 days. Escalation to management required.',
     'PERFORMANCE', 'CLM-2026-FC-009', 'READ', '2026-04-06 09:00:00', '2026-04-06 10:15:00'),
(8,  3, 'SLA Warning - Documentation Delay',
     'Adjuster 4 (Lisa Chen) has 3 open documentation delays. Average overdue: 7 days.',
     'PERFORMANCE', NULL, 'UNREAD', '2026-02-20 11:00:00', NULL),
(9,  1, 'SLA Breach - Inspection Overdue',
     'Claim CLM-2026-PROP-002 inspection is 11 days overdue. Fire damage assessment has not commenced.',
     'PERFORMANCE', 'CLM-2026-PROP-002', 'READ', '2026-01-29 08:30:00', '2026-01-29 09:45:00'),
(10, 3, 'Adjuster SLA Summary - Q1 2026',
     'Adjuster 1 (Mike Torres): 4 SLA breaches this quarter. Adjuster 3 (James Park): 5 SLA breaches. Review recommended.',
     'PERFORMANCE', NULL, 'READ', '2026-04-02 09:00:00', '2026-04-02 11:00:00'),
-- DENIAL alerts
(11, 2, 'Denial Pattern Alert - Medical Claims',
     'Denial code D005 (treatment not medically necessary) has been applied to 2 medical claims this month. Clinical review protocol may need updating.',
     'DENIAL', NULL, 'UNREAD', '2026-02-22 10:00:00', NULL),
(12, 2, 'High Denial Rate - Liability Claims',
     'Liability claims denial rate reached 28% this quarter, up from 18% in Q4-2025. Review denial criteria.',
     'DENIAL', NULL, 'READ', '2026-04-02 08:00:00', '2026-04-02 09:30:00'),
(13, 3, 'Denial Reversal Request - CLM-2026-FC-009',
     'Claimant''s solicitor has filed a formal objection to denial D009 (assumed risk). Legal review requested within 10 business days.',
     'DENIAL', 'CLM-2026-FC-009', 'UNREAD', '2026-03-15 14:00:00', NULL),
(14, 2, 'Denial Trend Report Available',
     'Monthly denial trend report for March 2026 is ready. Top denial codes: D002 (pre-existing), D005 (not medically necessary), D007 (liability).',
     'DENIAL', NULL, 'DISMISSED', '2026-04-01 08:00:00', NULL),
-- COST alerts
(15, 5, 'Reserve Adequacy Warning - CLM-ENT-001',
     'Current reserve of £400,000 for CLM-ENT-001 may be insufficient. Updated legal assessment suggests exposure up to £650,000.',
     'COST', 'CLM-ENT-001', 'UNREAD', '2026-03-10 09:00:00', NULL),
(16, 5, 'Total Reserve Threshold Exceeded',
     'Portfolio total reserve has exceeded £1.2M for the first time this year. Actuarial review of IBNR reserves recommended.',
     'COST', NULL, 'READ', '2026-03-12 10:00:00', '2026-03-12 14:00:00'),
(17, 3, 'High Cost Claim - CLM-2026-PROP-002',
     'Property claim CLM-2026-PROP-002 costs have reached £45,000. Contractor quotes variance of 18% requires second assessment.',
     'COST', 'CLM-2026-PROP-002', 'READ', '2026-02-10 08:30:00', '2026-02-10 09:00:00'),
(18, 5, 'Leakage Detection - Overpayment Identified',
     'Financial audit identified £45,000 overpayment on CLM-ENT-001. Recovery process to be initiated.',
     'COST', 'CLM-ENT-001', 'UNREAD', '2026-03-06 11:00:00', NULL),
-- AGING alerts
(19, 2, 'Aging Alert - 5 Claims Over 90 Days',
     'Claims CLM-2026-FC-006, CLM-2026-FC-009, CLM-ENT-001, CLM-ENT-002, CLM-ENT-003 have exceeded 90 days without resolution.',
     'AGING', NULL, 'UNREAD', '2026-04-05 08:00:00', NULL),
(20, 2, 'Aging Warning - CLM-2026-PROP-002',
     'Property fire claim CLM-2026-PROP-002 has been open for 78 days. Target closure is 90 days.',
     'AGING', 'CLM-2026-PROP-002', 'READ', '2026-04-10 09:00:00', '2026-04-10 10:30:00'),
(21, 3, 'Aging Report - Q1 2026 Summary',
     '5 claims in BUCKET_90_PLUS representing £985,000 total reserve. Average age: 112 days. Immediate management action required.',
     'AGING', NULL, 'UNREAD', '2026-04-01 09:00:00', NULL),
-- SYSTEM alerts
(22, 1, 'Data Feed Failure - LegacyClaims v1',
     'Data feed from LegacyClaims v1 (Feed ID: 9) has been in FAILED status since 2024-07-01. IT team notified.',
     'SYSTEM', '9', 'READ', '2026-01-15 08:00:00', '2026-01-15 09:00:00'),
(23, 1, 'Scheduled Maintenance - 2026-04-20 02:00',
     'The ClaimInsight360 platform will be unavailable for maintenance on 20 April 2026 from 02:00 to 04:00 UTC.',
     'SYSTEM', NULL, 'UNREAD', '2026-04-13 10:00:00', NULL),
(24, 1, 'New User Registered - bob_analyst',
     'New user bob_analyst (CLAIMS_ANALYST role) has registered on the platform.',
     'SYSTEM', NULL, 'READ', '2026-01-20 09:00:00', '2026-01-20 09:15:00'),
(25, 1, 'Eureka Service Down - analytics-report-service',
     'analytics-report-service has not sent a heartbeat for 5 minutes. Auto-restart attempted.',
     'SYSTEM', NULL, 'ACTIONED', '2026-02-28 03:15:00', '2026-02-28 07:00:00'),
(26, 4, 'Fraud Model Retrained - New Threshold Applied',
     'The fraud risk scoring model was retrained on 2026-03-01 with Q4-2025 + Q1-2026 data. High-risk threshold remains at 75.',
     'SYSTEM', NULL, 'READ', '2026-03-01 09:00:00', '2026-03-01 10:00:00'),
(27, 1, 'Critical SLA Violation - Adjuster 1',
     'Adjuster 1 (Mike Torres) has a CRITICAL SLA violation (INSPECTION_DELAY) with 11 days overdue for claim CLM-2026-PROP-002.',
     'PERFORMANCE', 'CLM-2026-PROP-002', 'READ', '2026-01-29 08:35:00', '2026-01-29 11:00:00'),
(28, 2, 'Denial Pattern - Policy Exclusion Spike',
     'Denial code D014 (flood zone exclusion) applied to 3 claims in January 2026. Product team alerted to review exclusion wording.',
     'DENIAL', NULL, 'UNREAD', '2026-02-03 10:00:00', NULL),
(29, 5, 'IBNR Reserve Adjustment Required',
     'Actuarial model suggests IBNR reserves should increase by 12% based on Q1-2026 development factors.',
     'COST', NULL, 'UNREAD', '2026-04-07 09:00:00', NULL),
(30, 3, 'Q1-2026 Performance Dashboard Ready',
     'The Q1-2026 analytics report has been generated. Key finding: loss ratio 73.4%, SLA breach rate 18%, fraud flag rate 12%.',
     'SYSTEM', NULL, 'UNREAD', '2026-04-01 10:00:00', NULL);

-- ── sla_violations mock (12 rows) ────────────────────────────────────
INSERT IGNORE INTO sla_violations
  (violation_id, claim_id, adjuster_id, violation_type, sla_target_days, actual_days, days_overdue, violation_date)
VALUES
(1,  'CLM-2026-AUTO-003', 3, 'RESPONSE_DELAY',      2,  5,  3,  '2026-01-14'),
(2,  'CLM-2026-PROP-001', 1, 'RESPONSE_DELAY',      2,  4,  2,  '2026-01-22'),
(3,  'CLM-2026-PROP-002', 1, 'INSPECTION_DELAY',    7,  18, 11, '2026-01-29'),
(4,  'CLM-2026-MED-001',  4, 'RESPONSE_DELAY',      2,  4,  2,  '2026-02-03'),
(5,  'CLM-2026-FC-006',   3, 'RESPONSE_DELAY',      2,  7,  5,  '2026-02-05'),
(6,  'CLM-2026-AUTO-003', 3, 'SETTLEMENT_BREACH',   30, 45, 15, '2026-02-11'),
(7,  'CLM-2026-PROP-002', 1, 'SETTLEMENT_BREACH',   30, 62, 32, '2026-02-21'),
(8,  'CLM-2026-FC-009',   3, 'SETTLEMENT_BREACH',   30, 75, 45, '2026-05-21'),
(9,  'CLM-ENT-001',       3, 'SETTLEMENT_BREACH',   30, 90, 60, '2026-05-04'),
(10, 'CLM-2026-AUTO-002', 2, 'DOCUMENTATION_DELAY', 5,  9,  4,  '2026-01-13'),
(11, 'CLM-2026-MED-003',  4, 'DOCUMENTATION_DELAY', 5,  12, 7,  '2026-02-15'),
(12, 'CLM-2026-FC-009',   3, 'INSPECTION_DELAY',    7,  22, 15, '2026-02-10');

-- ── risk_scores mock (15 rows) ────────────────────────────────────────
INSERT IGNORE INTO risk_scores
  (score_id, claim_id, score_value, risk_level, computed_date)
VALUES
(1,  'CLM-2026-AUTO-001', 23,  'LOW',      '2026-01-07 09:00:00'),
(2,  'CLM-2026-AUTO-003', 58,  'MEDIUM',   '2026-01-13 09:00:00'),
(3,  'CLM-2026-PROP-002', 89,  'HIGH',     '2026-01-23 09:00:00'),
(4,  'CLM-2026-MED-001',  52,  'MEDIUM',   '2026-02-02 09:00:00'),
(5,  'CLM-2026-FC-004',   78,  'HIGH',     '2026-01-21 09:00:00'),
(6,  'CLM-2026-FC-006',   95,  'CRITICAL', '2026-02-04 09:00:00'),
(7,  'CLM-2026-FC-009',   82,  'HIGH',     '2026-02-21 09:00:00'),
(8,  'CLM-ENT-001',       91,  'CRITICAL', '2026-02-21 09:00:00'),
(9,  'CLM-ENT-002',       79,  'HIGH',     '2026-02-22 09:00:00'),
(10, 'CLM-ENT-003',       98,  'CRITICAL', '2026-03-01 09:00:00'),
(11, 'CLM-2026-AUTO-004', 32,  'LOW',      '2026-01-16 09:00:00'),
(12, 'CLM-2026-PROP-001', 44,  'MEDIUM',   '2026-01-21 09:00:00'),
(13, 'CLM-2026-FC-002',   46,  'MEDIUM',   '2026-01-11 09:00:00'),
(14, 'CLM-2026-FC-003',   69,  'MEDIUM',   '2026-01-17 09:00:00'),
(15, 'CLM-2026-FC-008',   55,  'MEDIUM',   '2026-02-13 09:00:00');

-- ── aging_records mock (12 rows) ─────────────────────────────────────
INSERT IGNORE INTO aging_records
  (aging_id, claim_id, aging_days, aging_bucket, as_of_date, status)
VALUES
(1,  'CLM-2026-AUTO-001', 15,  '0-30',   '2026-03-01', 'OPEN'),
(2,  'CLM-2026-AUTO-003', 45,  '31-60',  '2026-03-01', 'IN_REVIEW'),
(3,  'CLM-2026-PROP-001', 55,  '31-60',  '2026-03-01', 'OPEN'),
(4,  'CLM-2026-PROP-002', 78,  '61-90',  '2026-03-01', 'IN_REVIEW'),
(5,  'CLM-2026-MED-001',  35,  '31-60',  '2026-03-01', 'OPEN'),
(6,  'CLM-2026-MED-003',  62,  '61-90',  '2026-03-01', 'IN_REVIEW'),
(7,  'CLM-2026-FC-006',   95,  '90+',    '2026-03-01', 'OPEN'),
(8,  'CLM-2026-FC-009',   110, '90+',    '2026-03-01', 'OPEN'),
(9,  'CLM-ENT-001',       130, '90+',    '2026-03-01', 'PENDING'),
(10, 'CLM-ENT-002',       105, '90+',    '2026-03-01', 'PENDING'),
(11, 'CLM-ENT-003',       120, '90+',    '2026-03-01', 'OPEN'),
(12, 'CLM-2026-FC-003',   42,  '31-60',  '2026-03-01', 'OPEN');


-- ====================================================================
-- Done.  All 7 databases seeded.
-- ====================================================================
SELECT 'Seed complete' AS status;
