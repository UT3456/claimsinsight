-- ============================================================
-- ClaimInsight360 - Claims Metrics Engine
-- data.sql  — Seed / Mock Data
--
-- Runs automatically on startup only when application.yml has:
--   spring.jpa.hibernate.ddl-auto: none
--   spring.sql.init.mode: always
--
-- schema.sql runs FIRST (creates tables), this file runs SECOND.
-- Uses INSERT IGNORE — safe to re-run on existing database.
-- ============================================================

-- ============================================================
-- SEED: claim_kpi (75 rows)
--
-- 15 distinct claims:
--   CLM-001 through CLM-010  — standard claims
--   CLM-ENT-001 through CLM-ENT-005  — enterprise/high-value claims
--
-- Each claim has all 5 KPI metrics:
--   TAT (Turnaround Time, days)
--   CYCLE_TIME (total workflow days)
--   SEVERITY (impact score 1-10)
--   FREQUENCY (occurrence count in period)
--   LOSS_RATIO (claims paid / premiums, decimal)
--
-- Date range: Jan 15 – Mar 10, 2026
-- ============================================================

-- CLM-001: Standard auto claim — fast resolution, low severity
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(1,  'CLM-001', 'TAT',        5.0000, '2026-01-15'),
(2,  'CLM-001', 'CYCLE_TIME', 7.5000, '2026-01-15'),
(3,  'CLM-001', 'SEVERITY',   2.5000, '2026-01-15'),
(4,  'CLM-001', 'FREQUENCY',  1.0000, '2026-01-15'),
(5,  'CLM-001', 'LOSS_RATIO', 0.4200, '2026-01-15');

-- CLM-002: Property water damage — medium severity, higher loss ratio
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(6,  'CLM-002', 'TAT',        12.0000, '2026-01-16'),
(7,  'CLM-002', 'CYCLE_TIME', 18.0000, '2026-01-16'),
(8,  'CLM-002', 'SEVERITY',   5.0000,  '2026-01-16'),
(9,  'CLM-002', 'FREQUENCY',  2.0000,  '2026-01-16'),
(10, 'CLM-002', 'LOSS_RATIO', 0.6800,  '2026-01-16');

-- CLM-003: Medical claim — high severity, long cycle
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(11, 'CLM-003', 'TAT',        21.0000, '2026-01-17'),
(12, 'CLM-003', 'CYCLE_TIME', 35.0000, '2026-01-17'),
(13, 'CLM-003', 'SEVERITY',   7.0000,  '2026-01-17'),
(14, 'CLM-003', 'FREQUENCY',  1.0000,  '2026-01-17'),
(15, 'CLM-003', 'LOSS_RATIO', 0.8500,  '2026-01-17');

-- CLM-004: Auto total loss — critical severity
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(16, 'CLM-004', 'TAT',        8.0000, '2026-01-20'),
(17, 'CLM-004', 'CYCLE_TIME', 14.0000, '2026-01-20'),
(18, 'CLM-004', 'SEVERITY',   9.0000,  '2026-01-20'),
(19, 'CLM-004', 'FREQUENCY',  1.0000,  '2026-01-20'),
(20, 'CLM-004', 'LOSS_RATIO', 0.9200,  '2026-01-20');

-- CLM-005: Minor windscreen claim — very low severity, excellent TAT
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(21, 'CLM-005', 'TAT',        1.0000, '2026-01-22'),
(22, 'CLM-005', 'CYCLE_TIME', 1.5000, '2026-01-22'),
(23, 'CLM-005', 'SEVERITY',   0.5000, '2026-01-22'),
(24, 'CLM-005', 'FREQUENCY',  3.0000, '2026-01-22'),
(25, 'CLM-005', 'LOSS_RATIO', 0.3500, '2026-01-22');

-- CLM-006: Property fire — very high severity, under investigation
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(26, 'CLM-006', 'TAT',        45.0000, '2026-01-25'),
(27, 'CLM-006', 'CYCLE_TIME', 90.0000, '2026-01-25'),
(28, 'CLM-006', 'SEVERITY',   9.5000,  '2026-01-25'),
(29, 'CLM-006', 'FREQUENCY',  1.0000,  '2026-01-25'),
(30, 'CLM-006', 'LOSS_RATIO', 1.1000,  '2026-01-25');

-- CLM-007: Liability slip and fall — medium severity
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(31, 'CLM-007', 'TAT',        30.0000, '2026-02-01'),
(32, 'CLM-007', 'CYCLE_TIME', 60.0000, '2026-02-01'),
(33, 'CLM-007', 'SEVERITY',   6.0000,  '2026-02-01'),
(34, 'CLM-007', 'FREQUENCY',  2.0000,  '2026-02-01'),
(35, 'CLM-007', 'LOSS_RATIO', 0.7500,  '2026-02-01');

-- CLM-008: Fleet claim — medium TAT, recurring frequency
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(36, 'CLM-008', 'TAT',        6.0000, '2026-02-05'),
(37, 'CLM-008', 'CYCLE_TIME', 9.0000, '2026-02-05'),
(38, 'CLM-008', 'SEVERITY',   3.0000, '2026-02-05'),
(39, 'CLM-008', 'FREQUENCY',  7.0000, '2026-02-05'),
(40, 'CLM-008', 'LOSS_RATIO', 0.5500, '2026-02-05');

-- CLM-009: Medical surgery — high value, long TAT
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(41, 'CLM-009', 'TAT',        25.0000, '2026-02-10'),
(42, 'CLM-009', 'CYCLE_TIME', 40.0000, '2026-02-10'),
(43, 'CLM-009', 'SEVERITY',   8.0000,  '2026-02-10'),
(44, 'CLM-009', 'FREQUENCY',  1.0000,  '2026-02-10'),
(45, 'CLM-009', 'LOSS_RATIO', 0.9000,  '2026-02-10');

-- CLM-010: Theft claim — quick resolution
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(46, 'CLM-010', 'TAT',        4.0000, '2026-02-15'),
(47, 'CLM-010', 'CYCLE_TIME', 6.0000, '2026-02-15'),
(48, 'CLM-010', 'SEVERITY',   4.5000, '2026-02-15'),
(49, 'CLM-010', 'FREQUENCY',  4.0000, '2026-02-15'),
(50, 'CLM-010', 'LOSS_RATIO', 0.6000, '2026-02-15');

-- CLM-ENT-001: Product liability — catastrophic, long-running
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(51, 'CLM-ENT-001', 'TAT',        120.0000, '2026-02-20'),
(52, 'CLM-ENT-001', 'CYCLE_TIME', 365.0000, '2026-02-20'),
(53, 'CLM-ENT-001', 'SEVERITY',   10.0000,  '2026-02-20'),
(54, 'CLM-ENT-001', 'FREQUENCY',  1.0000,   '2026-02-20'),
(55, 'CLM-ENT-001', 'LOSS_RATIO', 1.5000,   '2026-02-20');

-- CLM-ENT-002: Employer liability scaffold fall
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(56, 'CLM-ENT-002', 'TAT',        60.0000, '2026-02-22'),
(57, 'CLM-ENT-002', 'CYCLE_TIME', 180.0000,'2026-02-22'),
(58, 'CLM-ENT-002', 'SEVERITY',   9.0000,  '2026-02-22'),
(59, 'CLM-ENT-002', 'FREQUENCY',  1.0000,  '2026-02-22'),
(60, 'CLM-ENT-002', 'LOSS_RATIO', 1.2000,  '2026-02-22');

-- CLM-ENT-003: Cyber incident — large enterprise
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(61, 'CLM-ENT-003', 'TAT',        90.0000, '2026-03-01'),
(62, 'CLM-ENT-003', 'CYCLE_TIME', 270.0000,'2026-03-01'),
(63, 'CLM-ENT-003', 'SEVERITY',   9.8000,  '2026-03-01'),
(64, 'CLM-ENT-003', 'FREQUENCY',  1.0000,  '2026-03-01'),
(65, 'CLM-ENT-003', 'LOSS_RATIO', 1.8000,  '2026-03-01');

-- CLM-ENT-004: Fleet multiple vehicle accident
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(66, 'CLM-ENT-004', 'TAT',        15.0000, '2026-03-05'),
(67, 'CLM-ENT-004', 'CYCLE_TIME', 30.0000, '2026-03-05'),
(68, 'CLM-ENT-004', 'SEVERITY',   7.5000,  '2026-03-05'),
(69, 'CLM-ENT-004', 'FREQUENCY',  12.0000, '2026-03-05'),
(70, 'CLM-ENT-004', 'LOSS_RATIO', 0.8800,  '2026-03-05');

-- CLM-ENT-005: Commercial property storm damage
INSERT IGNORE INTO claim_kpi (kpi_id, claim_id, metric_name, metric_value, metric_date) VALUES
(71, 'CLM-ENT-005', 'TAT',        20.0000, '2026-03-10'),
(72, 'CLM-ENT-005', 'CYCLE_TIME', 45.0000, '2026-03-10'),
(73, 'CLM-ENT-005', 'SEVERITY',   6.5000,  '2026-03-10'),
(74, 'CLM-ENT-005', 'FREQUENCY',  5.0000,  '2026-03-10'),
(75, 'CLM-ENT-005', 'LOSS_RATIO', 0.7200,  '2026-03-10');
