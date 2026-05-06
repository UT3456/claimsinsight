-- ============================================================================
-- ClaimInsight360 — Realistic Seed Data
--
-- Run AFTER all services have started at least once (so JPA's ddl-auto=update
-- has created the tables). Each service owns its own MySQL database; this
-- file uses USE statements to switch databases as it goes.
--
--   $ "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot < seed_data.sql
--
-- What you get:
--   • 6 users (one per role) + 6 mirrored rows in NotificationsDB.users
--   • 5 data feeds (4 ACTIVE, 1 INACTIVE)
--   • 40 claims spread across AUTO / PROPERTY / HEALTH / LIABILITY / WC,
--     filed between 2025-08 and 2026-05
--   • 240 KPI rows (6 metrics × 40 claims)
--   • 28 risk scores (skewed toward 60-95 so the Fraud dashboard has signal)
--   • 35 risk indicators across HighCost / UnusualTiming / Pattern
--   • 6 SIU investigations (claims metrics fraud flow demo)
--   • 14 denial patterns + 8 leakage flags
--   • 32 adjuster-performance rows (8 adjusters × 4 quarters)
--   • 18 SLA violations
--   • 110 cost rows + 40 reserves + 40 aging records
--   • 24 analytics reports
--   • 95 notifications across all users
--
-- All passwords are BCrypt of the literal string "Password1!" so every seeded
-- user can log in immediately:  Password1!
-- (hash: $2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.)
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS data_ingestion_db          CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS claims_metrics_db          CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS fraud_risk_db              CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS denial_leakage             CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS claims_cost_reserve_db     CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS AdjusterPerformanceDB      CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS NotificationsDB            CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS claims_analytics_report_db CHARACTER SET utf8mb4;

-- ============================================================================
-- 1. API-GATEWAY  (uses whatever DB the gateway points to — table names are
--    gateway_users / audit_logs / refresh_tokens. Run this section ONLY
--    against that database.)
-- ============================================================================

-- The gateway's DB name is set in api-gateway/src/main/resources/application-dev.yml
-- (default: claiminsight_identity_db). If yours differs, change the USE below.
CREATE DATABASE IF NOT EXISTS claiminsight_identity_db CHARACTER SET utf8mb4;
USE claiminsight_identity_db;

-- Wipe & seed users
DELETE FROM refresh_tokens;
DELETE FROM gateway_users;
ALTER TABLE gateway_users AUTO_INCREMENT = 1;

INSERT INTO gateway_users (id, username, name, email, phone, password, role, enabled, created_at, updated_at) VALUES
(1, 'admin_alice',     'Alice Admin',         'alice.admin@acme-carrier.com',     '+1-202-555-0101', '$2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.', 'ROLE_ADMIN',           1, '2025-01-15 09:00:00', '2025-01-15 09:00:00'),
(2, 'sarah_analyst',   'Sarah Chen',          'sarah.chen@acme-carrier.com',      '+1-415-555-0102', '$2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.', 'ROLE_CLAIMS_ANALYST',  1, '2025-02-01 10:30:00', '2025-02-01 10:30:00'),
(3, 'david_manager',   'David Patel',         'david.patel@acme-carrier.com',     '+1-415-555-0103', '$2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.', 'ROLE_CLAIMS_MANAGER',  1, '2025-02-05 11:00:00', '2025-02-05 11:00:00'),
(4, 'priya_fraud',     'Priya Menon',         'priya.menon@acme-carrier.com',     '+1-510-555-0104', '$2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.', 'ROLE_FRAUD_ANALYST',   1, '2025-02-10 09:15:00', '2025-02-10 09:15:00'),
(5, 'nadia_actuary',   'Nadia Rashid',        'nadia.rashid@acme-carrier.com',    '+1-510-555-0105', '$2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.', 'ROLE_ACTUARY',         1, '2025-02-12 14:00:00', '2025-02-12 14:00:00'),
(6, 'lena_ops',        'Lena Garcia',         'lena.garcia@acme-carrier.com',     '+1-650-555-0106', '$2a$10$wH5oY8x3KuqRSrpc.eHQO.vKw.Rv4.cJg8ND4bIJJTxqZqDCkVgJ.', 'ROLE_OPERATIONS_EXEC', 1, '2025-02-15 09:30:00', '2025-02-15 09:30:00');

-- A handful of audit log entries so the page isn't empty on first open
DELETE FROM audit_logs;
ALTER TABLE audit_logs AUTO_INCREMENT = 1;
INSERT INTO audit_logs (user_id, username, action, resource, timestamp, metadata) VALUES
(1, 'admin_alice',   'LOGIN_SUCCESS', '/api/auth/login',  '2026-05-04 09:01:23', '{"method":"POST","status":200,"ip":"10.1.0.42"}'),
(2, 'sarah_analyst', 'LOGIN_SUCCESS', '/api/auth/login',  '2026-05-04 09:12:08', '{"method":"POST","status":200,"ip":"10.1.0.51"}'),
(3, 'david_manager', 'LOGIN_SUCCESS', '/api/auth/login',  '2026-05-04 09:33:45', '{"method":"POST","status":200,"ip":"10.1.0.62"}'),
(4, 'priya_fraud',   'LOGIN_SUCCESS', '/api/auth/login',  '2026-05-04 10:02:11', '{"method":"POST","status":200,"ip":"10.1.0.71"}'),
(5, 'nadia_actuary', 'LOGIN_SUCCESS', '/api/auth/login',  '2026-05-04 10:15:00', '{"method":"POST","status":200,"ip":"10.1.0.81"}'),
(6, 'lena_ops',      'LOGIN_SUCCESS', '/api/auth/login',  '2026-05-04 11:00:30', '{"method":"POST","status":200,"ip":"10.1.0.91"}'),
(1, 'admin_alice',   'REGISTER',      '/api/auth/register','2025-01-15 08:55:00', '{"role":"ROLE_ADMIN"}'),
(NULL, 'unknown',    'LOGIN_FAILED',  '/api/auth/login',  '2026-05-03 22:14:08', '{"reason":"wrong password"}'),
(NULL, 'unknown',    'LOGIN_FAILED',  '/api/auth/login',  '2026-05-03 22:14:22', '{"reason":"wrong password"}'),
(1, 'admin_alice',   'USER_UPDATED',  '/api/users/3',     '2026-05-02 15:22:00', '{"target":"david_manager","change":"role:ROLE_CLAIMS_ANALYST->ROLE_CLAIMS_MANAGER"}');

-- ============================================================================
-- 2. DATA INGESTION  (data_ingestion_db: data_feed, claim_raw)
-- ============================================================================
USE data_ingestion_db;
DELETE FROM claim_raw;
DELETE FROM data_feed;
ALTER TABLE data_feed AUTO_INCREMENT = 1;
ALTER TABLE claim_raw AUTO_INCREMENT = 1;

INSERT INTO data_feed (feed_id, feed_type, source_system, last_sync_date, status, created_date) VALUES
(1, 'CLAIM',   'GuideWire ClaimCenter',  '2026-05-05 06:00:12', 'ACTIVE',   '2025-01-10 09:00:00'),
(2, 'POLICY',  'Duck Creek Policy',      '2026-05-05 06:00:18', 'ACTIVE',   '2025-01-10 09:00:00'),
(3, 'PAYMENT', 'SAP Treasury',           '2026-05-05 06:00:30', 'ACTIVE',   '2025-01-12 10:00:00'),
(4, 'RESERVE', 'Internal Reserves Tool', '2026-05-04 06:00:00', 'ACTIVE',   '2025-01-15 11:00:00'),
(5, 'CLAIM',   'Legacy COBOL Mainframe', '2025-11-20 06:00:00', 'INACTIVE', '2025-01-08 08:30:00');

-- 40 raw claims spread across 9 months of filing dates
INSERT INTO claim_raw (claim_id, feed_id, payload_json, ingested_date) VALUES
('CLM-2026-AUTO-001', 1, '{"claimType":"AUTO","claimAmount":4500,"incidentDate":"2025-12-10","filedDate":"2025-12-15","policyStartDate":"2024-08-01","adjusterId":1}', '2025-12-15 06:05:00'),
('CLM-2026-AUTO-002', 1, '{"claimType":"AUTO","claimAmount":12800,"incidentDate":"2025-11-15","filedDate":"2025-11-20","policyStartDate":"2024-09-12","adjusterId":2}', '2025-11-20 06:05:00'),
('CLM-2026-AUTO-003', 1, '{"claimType":"AUTO","claimAmount":2300,"incidentDate":"2026-01-04","filedDate":"2026-01-08","policyStartDate":"2025-04-01","adjusterId":3}', '2026-01-08 06:05:00'),
('CLM-2026-AUTO-004', 1, '{"claimType":"AUTO","claimAmount":18900,"incidentDate":"2026-02-12","filedDate":"2026-02-14","policyStartDate":"2024-12-15","adjusterId":1}', '2026-02-14 06:05:00'),
('CLM-2026-AUTO-005', 1, '{"claimType":"AUTO","claimAmount":850,"incidentDate":"2026-03-02","filedDate":"2026-03-05","policyStartDate":"2023-11-20","adjusterId":4}', '2026-03-05 06:05:00'),
('CLM-2026-AUTO-006', 1, '{"claimType":"AUTO","claimAmount":47000,"incidentDate":"2026-03-18","filedDate":"2026-03-22","policyStartDate":"2024-02-10","adjusterId":2}', '2026-03-22 06:05:00'),
('CLM-2026-AUTO-007', 1, '{"claimType":"AUTO","claimAmount":6200,"incidentDate":"2026-04-08","filedDate":"2026-04-10","policyStartDate":"2025-01-15","adjusterId":5}', '2026-04-10 06:05:00'),
('CLM-2026-AUTO-008', 1, '{"claimType":"AUTO","claimAmount":15500,"incidentDate":"2026-04-19","filedDate":"2026-04-22","policyStartDate":"2024-07-01","adjusterId":3}', '2026-04-22 06:05:00'),
('CLM-2026-PROP-001', 1, '{"claimType":"PROPERTY","claimAmount":85000,"incidentDate":"2025-09-04","filedDate":"2025-09-10","policyStartDate":"2024-01-15","adjusterId":1,"policyLimit":150000}', '2025-09-10 06:05:00'),
('CLM-2026-PROP-002', 1, '{"claimType":"PROPERTY","claimAmount":22300,"incidentDate":"2025-10-12","filedDate":"2025-10-15","policyStartDate":"2024-05-20","adjusterId":6,"policyLimit":100000}', '2025-10-15 06:05:00'),
('CLM-2026-PROP-003', 1, '{"claimType":"PROPERTY","claimAmount":175000,"incidentDate":"2025-11-08","filedDate":"2025-11-12","policyStartDate":"2025-10-01","adjusterId":2,"policyLimit":250000}', '2025-11-12 06:05:00'),
('CLM-2026-PROP-004', 1, '{"claimType":"PROPERTY","claimAmount":9800,"incidentDate":"2026-01-22","filedDate":"2026-01-26","policyStartDate":"2024-09-12","adjusterId":4,"policyLimit":120000}', '2026-01-26 06:05:00'),
('CLM-2026-PROP-005', 1, '{"claimType":"PROPERTY","claimAmount":62500,"incidentDate":"2026-02-28","filedDate":"2026-03-02","policyStartDate":"2024-11-15","adjusterId":7,"policyLimit":180000}', '2026-03-02 06:05:00'),
('CLM-2026-PROP-006', 1, '{"claimType":"PROPERTY","claimAmount":31000,"incidentDate":"2026-03-15","filedDate":"2026-03-19","policyStartDate":"2025-02-01","adjusterId":1,"policyLimit":100000}', '2026-03-19 06:05:00'),
('CLM-2026-PROP-007', 1, '{"claimType":"PROPERTY","claimAmount":248000,"incidentDate":"2026-04-02","filedDate":"2026-04-05","policyStartDate":"2024-06-10","adjusterId":3,"policyLimit":300000,"providerId":"PRV-MIAMI-041","billingCode":"BC-7733"}', '2026-04-05 06:05:00'),
('CLM-2026-PROP-008', 1, '{"claimType":"PROPERTY","claimAmount":4400,"incidentDate":"2026-04-25","filedDate":"2026-04-28","policyStartDate":"2025-04-15","adjusterId":5,"policyLimit":75000}', '2026-04-28 06:05:00'),
('CLM-2026-HLTH-001', 1, '{"claimType":"HEALTH","claimAmount":3200,"incidentDate":"2025-08-10","filedDate":"2025-08-14","policyStartDate":"2024-01-01","adjusterId":2,"policyLimit":50000}', '2025-08-14 06:05:00'),
('CLM-2026-HLTH-002', 1, '{"claimType":"HEALTH","claimAmount":14500,"admissionDate":"2025-09-22","filedDate":"2025-09-28","policyStartDate":"2024-01-01","adjusterId":4,"policyLimit":75000}', '2025-09-28 06:05:00'),
('CLM-2026-HLTH-003', 1, '{"claimType":"HEALTH","claimAmount":98000,"admissionDate":"2025-10-30","filedDate":"2025-11-05","policyStartDate":"2024-06-15","adjusterId":1,"policyLimit":100000,"providerId":"PRV-MIAMI-041","billingCode":"BC-7733"}', '2025-11-05 06:05:00'),
('CLM-2026-HLTH-004', 1, '{"claimType":"HEALTH","claimAmount":7200,"admissionDate":"2025-12-08","filedDate":"2025-12-12","policyStartDate":"2025-11-20","adjusterId":6,"policyLimit":50000}', '2025-12-12 06:05:00'),
('CLM-2026-HLTH-005', 1, '{"claimType":"HEALTH","claimAmount":42000,"admissionDate":"2026-01-15","filedDate":"2026-01-19","policyStartDate":"2024-03-10","adjusterId":3,"policyLimit":75000,"denialCode":"CO-4","status":"DENIED"}', '2026-01-19 06:05:00'),
('CLM-2026-HLTH-006', 1, '{"claimType":"HEALTH","claimAmount":2800,"admissionDate":"2026-02-22","filedDate":"2026-02-25","policyStartDate":"2024-08-01","adjusterId":7,"policyLimit":50000}', '2026-02-25 06:05:00'),
('CLM-2026-HLTH-007', 1, '{"claimType":"HEALTH","claimAmount":67500,"admissionDate":"2026-03-08","filedDate":"2026-03-12","policyStartDate":"2024-12-01","adjusterId":2,"policyLimit":80000,"providerId":"PRV-MIAMI-041","billingCode":"BC-7733"}', '2026-03-12 06:05:00'),
('CLM-2026-HLTH-008', 1, '{"claimType":"HEALTH","claimAmount":11200,"admissionDate":"2026-04-12","filedDate":"2026-04-15","policyStartDate":"2025-05-15","adjusterId":5,"policyLimit":60000}', '2026-04-15 06:05:00'),
('CLM-2026-LIAB-001', 1, '{"claimType":"LIABILITY","claimAmount":35000,"incidentDate":"2025-09-15","filedDate":"2025-09-20","policyStartDate":"2024-04-01","adjusterId":3,"policyLimit":500000}', '2025-09-20 06:05:00'),
('CLM-2026-LIAB-002', 1, '{"claimType":"LIABILITY","claimAmount":120000,"incidentDate":"2025-10-25","filedDate":"2025-11-02","policyStartDate":"2023-12-15","adjusterId":1,"policyLimit":1000000}', '2025-11-02 06:05:00'),
('CLM-2026-LIAB-003', 1, '{"claimType":"LIABILITY","claimAmount":68000,"incidentDate":"2026-01-12","filedDate":"2026-01-18","policyStartDate":"2024-08-20","adjusterId":4,"policyLimit":750000,"denialCode":"CO-16","status":"DENIED"}', '2026-01-18 06:05:00'),
('CLM-2026-LIAB-004', 1, '{"claimType":"LIABILITY","claimAmount":92000,"incidentDate":"2026-02-08","filedDate":"2026-02-12","policyStartDate":"2024-10-05","adjusterId":6,"policyLimit":500000}', '2026-02-12 06:05:00'),
('CLM-2026-LIAB-005', 1, '{"claimType":"LIABILITY","claimAmount":5800,"incidentDate":"2026-03-25","filedDate":"2026-03-28","policyStartDate":"2025-01-10","adjusterId":2,"policyLimit":250000}', '2026-03-28 06:05:00'),
('CLM-2026-LIAB-006', 1, '{"claimType":"LIABILITY","claimAmount":215000,"incidentDate":"2026-04-08","filedDate":"2026-04-14","policyStartDate":"2024-07-15","adjusterId":7,"policyLimit":1000000}', '2026-04-14 06:05:00'),
('CLM-2026-WC-001',   1, '{"claimType":"WC","claimAmount":12000,"incidentDate":"2025-08-22","filedDate":"2025-08-28","policyStartDate":"2024-01-01","adjusterId":5,"policyLimit":100000}', '2025-08-28 06:05:00'),
('CLM-2026-WC-002',   1, '{"claimType":"WC","claimAmount":48500,"incidentDate":"2025-10-10","filedDate":"2025-10-18","policyStartDate":"2024-01-01","adjusterId":3,"policyLimit":100000}', '2025-10-18 06:05:00'),
('CLM-2026-WC-003',   1, '{"claimType":"WC","claimAmount":7500,"incidentDate":"2025-11-25","filedDate":"2025-11-29","policyStartDate":"2024-06-01","adjusterId":8,"policyLimit":75000}', '2025-11-29 06:05:00'),
('CLM-2026-WC-004',   1, '{"claimType":"WC","claimAmount":105000,"incidentDate":"2026-01-08","filedDate":"2026-01-15","policyStartDate":"2024-09-01","adjusterId":1,"policyLimit":150000,"denialCode":"CO-29"}', '2026-01-15 06:05:00'),
('CLM-2026-WC-005',   1, '{"claimType":"WC","claimAmount":3400,"incidentDate":"2026-02-18","filedDate":"2026-02-22","policyStartDate":"2025-03-15","adjusterId":4,"policyLimit":50000}', '2026-02-22 06:05:00'),
('CLM-2026-WC-006',   1, '{"claimType":"WC","claimAmount":29000,"incidentDate":"2026-03-12","filedDate":"2026-03-16","policyStartDate":"2024-12-10","adjusterId":6,"policyLimit":100000}', '2026-03-16 06:05:00'),
('CLM-2026-WC-007',   1, '{"claimType":"WC","claimAmount":18500,"incidentDate":"2026-04-20","filedDate":"2026-04-24","policyStartDate":"2025-06-01","adjusterId":2,"policyLimit":75000}', '2026-04-24 06:05:00'),
('CLM-2026-AUTO-009', 1, '{"claimType":"AUTO","claimAmount":3300,"incidentDate":"2026-04-28","filedDate":"2026-05-01","policyStartDate":"2025-08-15","adjusterId":7}', '2026-05-01 06:05:00'),
('CLM-2026-AUTO-010', 1, '{"claimType":"AUTO","claimAmount":8800,"incidentDate":"2026-05-01","filedDate":"2026-05-03","policyStartDate":"2024-10-20","adjusterId":3}', '2026-05-03 06:05:00'),
('CLM-2026-PROP-009', 1, '{"claimType":"PROPERTY","claimAmount":54000,"incidentDate":"2026-04-30","filedDate":"2026-05-04","policyStartDate":"2024-08-25","adjusterId":5,"policyLimit":150000}', '2026-05-04 06:05:00');

-- ============================================================================
-- 3. CLAIMS METRICS  (claims_metrics_db: claim_kpi, claim_status)
--
-- 6 KPIs per claim × 40 claims = 240 rows. Values are realistic ranges:
--   TAT: 8-32 days     CYCLE_TIME: 12-45 days     SEVERITY: equal claimAmount
--   FREQUENCY: 0.8-2.5/yr  LOSS_RATIO: 55-92%   SETTLEMENT_TIME: 18-50 days
-- ============================================================================
USE claims_metrics_db;
DELETE FROM claim_kpi;
DELETE FROM claim_status;
ALTER TABLE claim_kpi AUTO_INCREMENT = 1;

-- TAT (days)
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2026-AUTO-001', 'TAT', 14.0, '2025-12-29'), ('CLM-2026-AUTO-002', 'TAT', 22.0, '2025-12-12'),
('CLM-2026-AUTO-003', 'TAT', 11.0, '2026-01-19'), ('CLM-2026-AUTO-004', 'TAT', 18.0, '2026-03-04'),
('CLM-2026-AUTO-005', 'TAT', 8.0,  '2026-03-13'), ('CLM-2026-AUTO-006', 'TAT', 27.0, '2026-04-18'),
('CLM-2026-AUTO-007', 'TAT', 13.0, '2026-04-23'), ('CLM-2026-AUTO-008', 'TAT', 16.0, '2026-05-08'),
('CLM-2026-PROP-001', 'TAT', 32.0, '2025-10-12'), ('CLM-2026-PROP-002', 'TAT', 21.0, '2025-11-05'),
('CLM-2026-PROP-003', 'TAT', 28.0, '2025-12-10'), ('CLM-2026-PROP-004', 'TAT', 15.0, '2026-02-10'),
('CLM-2026-PROP-005', 'TAT', 24.0, '2026-03-26'), ('CLM-2026-PROP-006', 'TAT', 19.0, '2026-04-07'),
('CLM-2026-PROP-007', 'TAT', 31.0, '2026-05-06'), ('CLM-2026-PROP-008', 'TAT', 12.0, '2026-05-10'),
('CLM-2026-HLTH-001', 'TAT', 16.0, '2025-08-30'), ('CLM-2026-HLTH-002', 'TAT', 19.0, '2025-10-17'),
('CLM-2026-HLTH-003', 'TAT', 26.0, '2025-12-01'), ('CLM-2026-HLTH-004', 'TAT', 14.0, '2025-12-26'),
('CLM-2026-HLTH-005', 'TAT', 30.0, '2026-02-18'), ('CLM-2026-HLTH-006', 'TAT', 11.0, '2026-03-08'),
('CLM-2026-HLTH-007', 'TAT', 23.0, '2026-04-04'), ('CLM-2026-HLTH-008', 'TAT', 17.0, '2026-05-02'),
('CLM-2026-LIAB-001', 'TAT', 25.0, '2025-10-15'), ('CLM-2026-LIAB-002', 'TAT', 29.0, '2025-12-01'),
('CLM-2026-LIAB-003', 'TAT', 20.0, '2026-02-07'), ('CLM-2026-LIAB-004', 'TAT', 23.0, '2026-03-07'),
('CLM-2026-LIAB-005', 'TAT', 12.0, '2026-04-09'), ('CLM-2026-LIAB-006', 'TAT', 28.0, '2026-05-12'),
('CLM-2026-WC-001', 'TAT', 18.0, '2025-09-15'), ('CLM-2026-WC-002', 'TAT', 24.0, '2025-11-11'),
('CLM-2026-WC-003', 'TAT', 13.0, '2025-12-12'), ('CLM-2026-WC-004', 'TAT', 31.0, '2026-02-15'),
('CLM-2026-WC-005', 'TAT', 10.0, '2026-03-04'), ('CLM-2026-WC-006', 'TAT', 19.0, '2026-04-04'),
('CLM-2026-WC-007', 'TAT', 16.0, '2026-05-10'), ('CLM-2026-AUTO-009', 'TAT', 9.0,  '2026-05-10'),
('CLM-2026-AUTO-010', 'TAT', 7.0,  '2026-05-10'), ('CLM-2026-PROP-009', 'TAT', 8.0,  '2026-05-12');

-- CYCLE_TIME (days)
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2026-AUTO-001', 'CYCLE_TIME', 21.0, '2025-12-29'), ('CLM-2026-AUTO-002', 'CYCLE_TIME', 28.0, '2025-12-12'),
('CLM-2026-AUTO-003', 'CYCLE_TIME', 18.0, '2026-01-19'), ('CLM-2026-AUTO-004', 'CYCLE_TIME', 25.0, '2026-03-04'),
('CLM-2026-AUTO-005', 'CYCLE_TIME', 12.0, '2026-03-13'), ('CLM-2026-AUTO-006', 'CYCLE_TIME', 35.0, '2026-04-18'),
('CLM-2026-AUTO-007', 'CYCLE_TIME', 19.0, '2026-04-23'), ('CLM-2026-AUTO-008', 'CYCLE_TIME', 22.0, '2026-05-08'),
('CLM-2026-PROP-001', 'CYCLE_TIME', 41.0, '2025-10-12'), ('CLM-2026-PROP-002', 'CYCLE_TIME', 28.0, '2025-11-05'),
('CLM-2026-PROP-003', 'CYCLE_TIME', 38.0, '2025-12-10'), ('CLM-2026-PROP-004', 'CYCLE_TIME', 22.0, '2026-02-10'),
('CLM-2026-PROP-005', 'CYCLE_TIME', 31.0, '2026-03-26'), ('CLM-2026-PROP-006', 'CYCLE_TIME', 26.0, '2026-04-07'),
('CLM-2026-PROP-007', 'CYCLE_TIME', 42.0, '2026-05-06'), ('CLM-2026-PROP-008', 'CYCLE_TIME', 17.0, '2026-05-10'),
('CLM-2026-HLTH-001', 'CYCLE_TIME', 22.0, '2025-08-30'), ('CLM-2026-HLTH-002', 'CYCLE_TIME', 26.0, '2025-10-17'),
('CLM-2026-HLTH-003', 'CYCLE_TIME', 34.0, '2025-12-01'), ('CLM-2026-HLTH-004', 'CYCLE_TIME', 19.0, '2025-12-26'),
('CLM-2026-HLTH-005', 'CYCLE_TIME', 38.0, '2026-02-18'), ('CLM-2026-HLTH-006', 'CYCLE_TIME', 16.0, '2026-03-08'),
('CLM-2026-HLTH-007', 'CYCLE_TIME', 30.0, '2026-04-04'), ('CLM-2026-HLTH-008', 'CYCLE_TIME', 23.0, '2026-05-02'),
('CLM-2026-LIAB-001', 'CYCLE_TIME', 32.0, '2025-10-15'), ('CLM-2026-LIAB-002', 'CYCLE_TIME', 38.0, '2025-12-01'),
('CLM-2026-LIAB-003', 'CYCLE_TIME', 27.0, '2026-02-07'), ('CLM-2026-LIAB-004', 'CYCLE_TIME', 30.0, '2026-03-07'),
('CLM-2026-LIAB-005', 'CYCLE_TIME', 17.0, '2026-04-09'), ('CLM-2026-LIAB-006', 'CYCLE_TIME', 36.0, '2026-05-12'),
('CLM-2026-WC-001', 'CYCLE_TIME', 25.0, '2025-09-15'), ('CLM-2026-WC-002', 'CYCLE_TIME', 31.0, '2025-11-11'),
('CLM-2026-WC-003', 'CYCLE_TIME', 19.0, '2025-12-12'), ('CLM-2026-WC-004', 'CYCLE_TIME', 40.0, '2026-02-15'),
('CLM-2026-WC-005', 'CYCLE_TIME', 14.0, '2026-03-04'), ('CLM-2026-WC-006', 'CYCLE_TIME', 25.0, '2026-04-04'),
('CLM-2026-WC-007', 'CYCLE_TIME', 22.0, '2026-05-10'), ('CLM-2026-AUTO-009', 'CYCLE_TIME', 14.0, '2026-05-10'),
('CLM-2026-AUTO-010', 'CYCLE_TIME', 11.0, '2026-05-10'), ('CLM-2026-PROP-009', 'CYCLE_TIME', 13.0, '2026-05-12');

-- SEVERITY (= claim amount in $)
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2026-AUTO-001', 'SEVERITY', 4500,   '2025-12-29'), ('CLM-2026-AUTO-002', 'SEVERITY', 12800,  '2025-12-12'),
('CLM-2026-AUTO-003', 'SEVERITY', 2300,   '2026-01-19'), ('CLM-2026-AUTO-004', 'SEVERITY', 18900,  '2026-03-04'),
('CLM-2026-AUTO-005', 'SEVERITY', 850,    '2026-03-13'), ('CLM-2026-AUTO-006', 'SEVERITY', 47000,  '2026-04-18'),
('CLM-2026-AUTO-007', 'SEVERITY', 6200,   '2026-04-23'), ('CLM-2026-AUTO-008', 'SEVERITY', 15500,  '2026-05-08'),
('CLM-2026-PROP-001', 'SEVERITY', 85000,  '2025-10-12'), ('CLM-2026-PROP-002', 'SEVERITY', 22300,  '2025-11-05'),
('CLM-2026-PROP-003', 'SEVERITY', 175000, '2025-12-10'), ('CLM-2026-PROP-004', 'SEVERITY', 9800,   '2026-02-10'),
('CLM-2026-PROP-005', 'SEVERITY', 62500,  '2026-03-26'), ('CLM-2026-PROP-006', 'SEVERITY', 31000,  '2026-04-07'),
('CLM-2026-PROP-007', 'SEVERITY', 248000, '2026-05-06'), ('CLM-2026-PROP-008', 'SEVERITY', 4400,   '2026-05-10'),
('CLM-2026-HLTH-001', 'SEVERITY', 3200,   '2025-08-30'), ('CLM-2026-HLTH-002', 'SEVERITY', 14500,  '2025-10-17'),
('CLM-2026-HLTH-003', 'SEVERITY', 98000,  '2025-12-01'), ('CLM-2026-HLTH-004', 'SEVERITY', 7200,   '2025-12-26'),
('CLM-2026-HLTH-005', 'SEVERITY', 42000,  '2026-02-18'), ('CLM-2026-HLTH-006', 'SEVERITY', 2800,   '2026-03-08'),
('CLM-2026-HLTH-007', 'SEVERITY', 67500,  '2026-04-04'), ('CLM-2026-HLTH-008', 'SEVERITY', 11200,  '2026-05-02'),
('CLM-2026-LIAB-001', 'SEVERITY', 35000,  '2025-10-15'), ('CLM-2026-LIAB-002', 'SEVERITY', 120000, '2025-12-01'),
('CLM-2026-LIAB-003', 'SEVERITY', 68000,  '2026-02-07'), ('CLM-2026-LIAB-004', 'SEVERITY', 92000,  '2026-03-07'),
('CLM-2026-LIAB-005', 'SEVERITY', 5800,   '2026-04-09'), ('CLM-2026-LIAB-006', 'SEVERITY', 215000, '2026-05-12'),
('CLM-2026-WC-001', 'SEVERITY', 12000,  '2025-09-15'),   ('CLM-2026-WC-002', 'SEVERITY', 48500,  '2025-11-11'),
('CLM-2026-WC-003', 'SEVERITY', 7500,   '2025-12-12'),   ('CLM-2026-WC-004', 'SEVERITY', 105000, '2026-02-15'),
('CLM-2026-WC-005', 'SEVERITY', 3400,   '2026-03-04'),   ('CLM-2026-WC-006', 'SEVERITY', 29000,  '2026-04-04'),
('CLM-2026-WC-007', 'SEVERITY', 18500,  '2026-05-10'),   ('CLM-2026-AUTO-009', 'SEVERITY', 3300,  '2026-05-10'),
('CLM-2026-AUTO-010', 'SEVERITY', 8800, '2026-05-10'),   ('CLM-2026-PROP-009', 'SEVERITY', 54000, '2026-05-12');

-- LOSS_RATIO (%)
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2026-AUTO-001', 'LOSS_RATIO', 62.5, '2025-12-29'), ('CLM-2026-AUTO-002', 'LOSS_RATIO', 71.2, '2025-12-12'),
('CLM-2026-AUTO-003', 'LOSS_RATIO', 58.0, '2026-01-19'), ('CLM-2026-AUTO-004', 'LOSS_RATIO', 76.4, '2026-03-04'),
('CLM-2026-AUTO-005', 'LOSS_RATIO', 55.0, '2026-03-13'), ('CLM-2026-AUTO-006', 'LOSS_RATIO', 88.3, '2026-04-18'),
('CLM-2026-AUTO-007', 'LOSS_RATIO', 64.8, '2026-04-23'), ('CLM-2026-AUTO-008', 'LOSS_RATIO', 73.1, '2026-05-08'),
('CLM-2026-PROP-001', 'LOSS_RATIO', 81.5, '2025-10-12'), ('CLM-2026-PROP-002', 'LOSS_RATIO', 69.0, '2025-11-05'),
('CLM-2026-PROP-003', 'LOSS_RATIO', 85.7, '2025-12-10'), ('CLM-2026-PROP-004', 'LOSS_RATIO', 60.2, '2026-02-10'),
('CLM-2026-PROP-005', 'LOSS_RATIO', 78.4, '2026-03-26'), ('CLM-2026-PROP-006', 'LOSS_RATIO', 71.0, '2026-04-07'),
('CLM-2026-PROP-007', 'LOSS_RATIO', 92.1, '2026-05-06'), ('CLM-2026-PROP-008', 'LOSS_RATIO', 56.8, '2026-05-10'),
('CLM-2026-HLTH-001', 'LOSS_RATIO', 64.0, '2025-08-30'), ('CLM-2026-HLTH-002', 'LOSS_RATIO', 70.5, '2025-10-17'),
('CLM-2026-HLTH-003', 'LOSS_RATIO', 89.4, '2025-12-01'), ('CLM-2026-HLTH-004', 'LOSS_RATIO', 65.2, '2025-12-26'),
('CLM-2026-HLTH-005', 'LOSS_RATIO', 84.3, '2026-02-18'), ('CLM-2026-HLTH-006', 'LOSS_RATIO', 57.1, '2026-03-08'),
('CLM-2026-HLTH-007', 'LOSS_RATIO', 86.5, '2026-04-04'), ('CLM-2026-HLTH-008', 'LOSS_RATIO', 68.9, '2026-05-02'),
('CLM-2026-LIAB-001', 'LOSS_RATIO', 74.0, '2025-10-15'), ('CLM-2026-LIAB-002', 'LOSS_RATIO', 82.6, '2025-12-01'),
('CLM-2026-LIAB-003', 'LOSS_RATIO', 78.0, '2026-02-07'), ('CLM-2026-LIAB-004', 'LOSS_RATIO', 80.1, '2026-03-07'),
('CLM-2026-LIAB-005', 'LOSS_RATIO', 60.5, '2026-04-09'), ('CLM-2026-LIAB-006', 'LOSS_RATIO', 91.2, '2026-05-12'),
('CLM-2026-WC-001', 'LOSS_RATIO', 67.5, '2025-09-15'), ('CLM-2026-WC-002', 'LOSS_RATIO', 79.8, '2025-11-11'),
('CLM-2026-WC-003', 'LOSS_RATIO', 62.4, '2025-12-12'), ('CLM-2026-WC-004', 'LOSS_RATIO', 90.6, '2026-02-15'),
('CLM-2026-WC-005', 'LOSS_RATIO', 58.2, '2026-03-04'), ('CLM-2026-WC-006', 'LOSS_RATIO', 73.0, '2026-04-04'),
('CLM-2026-WC-007', 'LOSS_RATIO', 70.8, '2026-05-10'), ('CLM-2026-AUTO-009', 'LOSS_RATIO', 56.0, '2026-05-10'),
('CLM-2026-AUTO-010', 'LOSS_RATIO', 65.0, '2026-05-10'), ('CLM-2026-PROP-009', 'LOSS_RATIO', 76.5, '2026-05-12');

-- FREQUENCY (claims per policy-year)
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2026-AUTO-001', 'FREQUENCY', 1.2, '2025-12-29'), ('CLM-2026-AUTO-002', 'FREQUENCY', 1.4, '2025-12-12'),
('CLM-2026-AUTO-003', 'FREQUENCY', 0.9, '2026-01-19'), ('CLM-2026-AUTO-004', 'FREQUENCY', 1.6, '2026-03-04'),
('CLM-2026-AUTO-005', 'FREQUENCY', 0.8, '2026-03-13'), ('CLM-2026-AUTO-006', 'FREQUENCY', 2.1, '2026-04-18'),
('CLM-2026-AUTO-007', 'FREQUENCY', 1.0, '2026-04-23'), ('CLM-2026-AUTO-008', 'FREQUENCY', 1.5, '2026-05-08'),
('CLM-2026-PROP-001', 'FREQUENCY', 0.6, '2025-10-12'), ('CLM-2026-PROP-002', 'FREQUENCY', 0.5, '2025-11-05'),
('CLM-2026-PROP-003', 'FREQUENCY', 0.4, '2025-12-10'), ('CLM-2026-PROP-004', 'FREQUENCY', 0.7, '2026-02-10'),
('CLM-2026-PROP-005', 'FREQUENCY', 0.5, '2026-03-26'), ('CLM-2026-PROP-006', 'FREQUENCY', 0.8, '2026-04-07'),
('CLM-2026-PROP-007', 'FREQUENCY', 0.3, '2026-05-06'), ('CLM-2026-PROP-008', 'FREQUENCY', 0.6, '2026-05-10'),
('CLM-2026-HLTH-001', 'FREQUENCY', 2.4, '2025-08-30'), ('CLM-2026-HLTH-002', 'FREQUENCY', 2.0, '2025-10-17'),
('CLM-2026-HLTH-003', 'FREQUENCY', 1.8, '2025-12-01'), ('CLM-2026-HLTH-004', 'FREQUENCY', 2.2, '2025-12-26'),
('CLM-2026-HLTH-005', 'FREQUENCY', 1.6, '2026-02-18'), ('CLM-2026-HLTH-006', 'FREQUENCY', 2.5, '2026-03-08'),
('CLM-2026-HLTH-007', 'FREQUENCY', 1.7, '2026-04-04'), ('CLM-2026-HLTH-008', 'FREQUENCY', 2.0, '2026-05-02'),
('CLM-2026-LIAB-001', 'FREQUENCY', 0.9, '2025-10-15'), ('CLM-2026-LIAB-002', 'FREQUENCY', 0.6, '2025-12-01'),
('CLM-2026-LIAB-003', 'FREQUENCY', 0.8, '2026-02-07'), ('CLM-2026-LIAB-004', 'FREQUENCY', 0.7, '2026-03-07'),
('CLM-2026-LIAB-005', 'FREQUENCY', 1.1, '2026-04-09'), ('CLM-2026-LIAB-006', 'FREQUENCY', 0.5, '2026-05-12'),
('CLM-2026-WC-001', 'FREQUENCY', 1.3, '2025-09-15'), ('CLM-2026-WC-002', 'FREQUENCY', 1.5, '2025-11-11'),
('CLM-2026-WC-003', 'FREQUENCY', 1.0, '2025-12-12'), ('CLM-2026-WC-004', 'FREQUENCY', 1.4, '2026-02-15'),
('CLM-2026-WC-005', 'FREQUENCY', 0.9, '2026-03-04'), ('CLM-2026-WC-006', 'FREQUENCY', 1.2, '2026-04-04'),
('CLM-2026-WC-007', 'FREQUENCY', 1.1, '2026-05-10'), ('CLM-2026-AUTO-009', 'FREQUENCY', 0.7, '2026-05-10'),
('CLM-2026-AUTO-010', 'FREQUENCY', 0.9, '2026-05-10'), ('CLM-2026-PROP-009', 'FREQUENCY', 0.4, '2026-05-12');

-- SETTLEMENT_TIME (days)
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2026-AUTO-001', 'SETTLEMENT_TIME', 28.0, '2025-12-29'), ('CLM-2026-AUTO-002', 'SETTLEMENT_TIME', 35.0, '2025-12-12'),
('CLM-2026-AUTO-003', 'SETTLEMENT_TIME', 22.0, '2026-01-19'), ('CLM-2026-AUTO-004', 'SETTLEMENT_TIME', 31.0, '2026-03-04'),
('CLM-2026-AUTO-005', 'SETTLEMENT_TIME', 18.0, '2026-03-13'), ('CLM-2026-AUTO-006', 'SETTLEMENT_TIME', 42.0, '2026-04-18'),
('CLM-2026-AUTO-007', 'SETTLEMENT_TIME', 24.0, '2026-04-23'), ('CLM-2026-AUTO-008', 'SETTLEMENT_TIME', 29.0, '2026-05-08'),
('CLM-2026-PROP-001', 'SETTLEMENT_TIME', 47.0, '2025-10-12'), ('CLM-2026-PROP-002', 'SETTLEMENT_TIME', 33.0, '2025-11-05'),
('CLM-2026-PROP-003', 'SETTLEMENT_TIME', 45.0, '2025-12-10'), ('CLM-2026-PROP-004', 'SETTLEMENT_TIME', 26.0, '2026-02-10'),
('CLM-2026-PROP-005', 'SETTLEMENT_TIME', 38.0, '2026-03-26'), ('CLM-2026-PROP-006', 'SETTLEMENT_TIME', 31.0, '2026-04-07'),
('CLM-2026-PROP-007', 'SETTLEMENT_TIME', 50.0, '2026-05-06'), ('CLM-2026-PROP-008', 'SETTLEMENT_TIME', 21.0, '2026-05-10'),
('CLM-2026-HLTH-001', 'SETTLEMENT_TIME', 27.0, '2025-08-30'), ('CLM-2026-HLTH-002', 'SETTLEMENT_TIME', 32.0, '2025-10-17'),
('CLM-2026-HLTH-003', 'SETTLEMENT_TIME', 41.0, '2025-12-01'), ('CLM-2026-HLTH-004', 'SETTLEMENT_TIME', 25.0, '2025-12-26'),
('CLM-2026-HLTH-005', 'SETTLEMENT_TIME', 46.0, '2026-02-18'), ('CLM-2026-HLTH-006', 'SETTLEMENT_TIME', 22.0, '2026-03-08'),
('CLM-2026-HLTH-007', 'SETTLEMENT_TIME', 37.0, '2026-04-04'), ('CLM-2026-HLTH-008', 'SETTLEMENT_TIME', 28.0, '2026-05-02'),
('CLM-2026-LIAB-001', 'SETTLEMENT_TIME', 38.0, '2025-10-15'), ('CLM-2026-LIAB-002', 'SETTLEMENT_TIME', 44.0, '2025-12-01'),
('CLM-2026-LIAB-003', 'SETTLEMENT_TIME', 33.0, '2026-02-07'), ('CLM-2026-LIAB-004', 'SETTLEMENT_TIME', 36.0, '2026-03-07'),
('CLM-2026-LIAB-005', 'SETTLEMENT_TIME', 23.0, '2026-04-09'), ('CLM-2026-LIAB-006', 'SETTLEMENT_TIME', 43.0, '2026-05-12'),
('CLM-2026-WC-001', 'SETTLEMENT_TIME', 30.0, '2025-09-15'), ('CLM-2026-WC-002', 'SETTLEMENT_TIME', 39.0, '2025-11-11'),
('CLM-2026-WC-003', 'SETTLEMENT_TIME', 24.0, '2025-12-12'), ('CLM-2026-WC-004', 'SETTLEMENT_TIME', 48.0, '2026-02-15'),
('CLM-2026-WC-005', 'SETTLEMENT_TIME', 19.0, '2026-03-04'), ('CLM-2026-WC-006', 'SETTLEMENT_TIME', 31.0, '2026-04-04'),
('CLM-2026-WC-007', 'SETTLEMENT_TIME', 27.0, '2026-05-10'), ('CLM-2026-AUTO-009', 'SETTLEMENT_TIME', 18.0, '2026-05-10'),
('CLM-2026-AUTO-010', 'SETTLEMENT_TIME', 16.0, '2026-05-10'), ('CLM-2026-PROP-009', 'SETTLEMENT_TIME', 17.0, '2026-05-12');

-- claim_status table — most ACTIVE, a few INACTIVE (settled)
INSERT INTO claim_status (claim_id, status, updated_at) VALUES
('CLM-2026-AUTO-001', 'INACTIVE', '2026-01-12 14:00:00'),
('CLM-2026-AUTO-002', 'INACTIVE', '2025-12-15 09:30:00'),
('CLM-2026-AUTO-003', 'ACTIVE',   '2026-01-19 10:00:00'),
('CLM-2026-AUTO-004', 'ACTIVE',   '2026-03-04 11:00:00'),
('CLM-2026-AUTO-005', 'INACTIVE', '2026-03-15 13:00:00'),
('CLM-2026-AUTO-006', 'ACTIVE',   '2026-04-18 16:00:00'),
('CLM-2026-PROP-001', 'INACTIVE', '2025-10-15 10:00:00'),
('CLM-2026-PROP-003', 'ACTIVE',   '2025-12-10 14:00:00'),
('CLM-2026-PROP-007', 'ACTIVE',   '2026-05-06 09:00:00'),
('CLM-2026-HLTH-005', 'ACTIVE',   '2026-02-18 11:30:00'),
('CLM-2026-LIAB-002', 'ACTIVE',   '2025-12-01 12:00:00'),
('CLM-2026-LIAB-006', 'ACTIVE',   '2026-05-12 15:00:00'),
('CLM-2026-WC-004',   'ACTIVE',   '2026-02-15 10:00:00');

-- ============================================================================
-- 4. FRAUD RISK  (fraud_risk_db: risk_score, risk_indicator, investigations)
-- ============================================================================
USE fraud_risk_db;
DELETE FROM investigations;
DELETE FROM risk_indicator;
DELETE FROM risk_score;
ALTER TABLE risk_score    AUTO_INCREMENT = 1;
ALTER TABLE risk_indicator AUTO_INCREMENT = 1;
ALTER TABLE investigations AUTO_INCREMENT = 1;

-- 28 risk scores — distribution skewed for a meaningful Fraud dashboard:
--   8 critical (90-99), 7 high (75-89), 8 mid (50-74), 5 low (15-49)
INSERT INTO risk_score (claim_id, score_value, computed_date) VALUES
('CLM-2026-PROP-007', 94.0, '2026-04-05'),  -- mill scheme suspect (PRV-MIAMI-041)
('CLM-2026-HLTH-003', 92.5, '2025-11-05'),  -- same provider
('CLM-2026-HLTH-007', 91.0, '2026-03-12'),  -- same provider
('CLM-2026-LIAB-006', 96.0, '2026-04-14'),  -- $215K + new policy
('CLM-2026-WC-004',   90.0, '2026-01-15'),  -- $105K denied
('CLM-2026-PROP-003', 93.0, '2025-11-12'),  -- new policy + $175K
('CLM-2026-LIAB-002', 91.5, '2025-11-02'),  -- $120K
('CLM-2026-AUTO-006', 95.0, '2026-03-22'),  -- $47K auto unusual
('CLM-2026-HLTH-005', 87.0, '2026-01-19'),  -- denied + $42K
('CLM-2026-PROP-005', 82.0, '2026-03-02'),
('CLM-2026-LIAB-004', 80.0, '2026-02-12'),
('CLM-2026-LIAB-003', 78.0, '2026-01-18'),  -- denied
('CLM-2026-PROP-001', 79.5, '2025-09-10'),
('CLM-2026-WC-002',   76.0, '2025-10-18'),
('CLM-2026-AUTO-008', 75.5, '2026-04-22'),
('CLM-2026-HLTH-008', 68.0, '2026-04-15'),
('CLM-2026-WC-006',   65.0, '2026-03-16'),
('CLM-2026-PROP-006', 64.0, '2026-03-19'),
('CLM-2026-LIAB-001', 62.0, '2025-09-20'),
('CLM-2026-AUTO-004', 58.0, '2026-02-14'),
('CLM-2026-HLTH-002', 55.0, '2025-09-28'),
('CLM-2026-AUTO-002', 52.0, '2025-11-20'),
('CLM-2026-WC-001',   50.0, '2025-08-28'),
('CLM-2026-PROP-002', 42.0, '2025-10-15'),
('CLM-2026-AUTO-007', 38.0, '2026-04-10'),
('CLM-2026-HLTH-001', 32.0, '2025-08-14'),
('CLM-2026-WC-003',   28.0, '2025-11-29'),
('CLM-2026-AUTO-005', 18.0, '2026-03-05');

-- 35 risk indicators
INSERT INTO risk_indicator (claim_id, indicator_type, severity, triggered_date) VALUES
('CLM-2026-PROP-007', 'Pattern',       'HIGH',     '2026-04-05'),
('CLM-2026-PROP-007', 'HighCost',      'CRITICAL', '2026-04-05'),
('CLM-2026-HLTH-003', 'Pattern',       'HIGH',     '2025-11-05'),
('CLM-2026-HLTH-003', 'HighCost',      'HIGH',     '2025-11-05'),
('CLM-2026-HLTH-007', 'Pattern',       'HIGH',     '2026-03-12'),
('CLM-2026-LIAB-006', 'HighCost',      'CRITICAL', '2026-04-14'),
('CLM-2026-LIAB-006', 'UnusualTiming', 'HIGH',     '2026-04-14'),
('CLM-2026-WC-004',   'HighCost',      'CRITICAL', '2026-01-15'),
('CLM-2026-PROP-003', 'HighCost',      'CRITICAL', '2025-11-12'),
('CLM-2026-PROP-003', 'UnusualTiming', 'HIGH',     '2025-11-12'),  -- policy started 2 mo before claim
('CLM-2026-LIAB-002', 'HighCost',      'HIGH',     '2025-11-02'),
('CLM-2026-AUTO-006', 'HighCost',      'HIGH',     '2026-03-22'),
('CLM-2026-AUTO-006', 'UnusualTiming', 'MEDIUM',   '2026-03-22'),
('CLM-2026-HLTH-005', 'Pattern',       'HIGH',     '2026-01-19'),
('CLM-2026-PROP-005', 'HighCost',      'HIGH',     '2026-03-02'),
('CLM-2026-LIAB-004', 'HighCost',      'HIGH',     '2026-02-12'),
('CLM-2026-LIAB-003', 'Pattern',       'MEDIUM',   '2026-01-18'),
('CLM-2026-PROP-001', 'HighCost',      'HIGH',     '2025-09-10'),
('CLM-2026-WC-002',   'HighCost',      'MEDIUM',   '2025-10-18'),
('CLM-2026-AUTO-008', 'UnusualTiming', 'MEDIUM',   '2026-04-22'),
('CLM-2026-HLTH-008', 'Pattern',       'MEDIUM',   '2026-04-15'),
('CLM-2026-WC-006',   'HighCost',      'MEDIUM',   '2026-03-16'),
('CLM-2026-PROP-006', 'UnusualTiming', 'MEDIUM',   '2026-03-19'),
('CLM-2026-LIAB-001', 'Pattern',       'MEDIUM',   '2025-09-20'),
('CLM-2026-AUTO-004', 'UnusualTiming', 'LOW',      '2026-02-14'),
('CLM-2026-HLTH-002', 'Pattern',       'LOW',      '2025-09-28'),
('CLM-2026-AUTO-002', 'HighCost',      'LOW',      '2025-11-20'),
('CLM-2026-WC-001',   'UnusualTiming', 'LOW',      '2025-08-28'),
('CLM-2026-PROP-002', 'Pattern',       'LOW',      '2025-10-15'),
('CLM-2026-AUTO-001', 'UnusualTiming', 'LOW',      '2025-12-15'),
('CLM-2026-PROP-007', 'UnusualTiming', 'HIGH',     '2026-04-05'),  -- 3rd indicator on the mill suspect
('CLM-2026-HLTH-007', 'HighCost',      'HIGH',     '2026-03-12'),
('CLM-2026-PROP-008', 'Pattern',       'LOW',      '2026-04-28'),
('CLM-2026-HLTH-006', 'UnusualTiming', 'LOW',      '2026-02-25'),
('CLM-2026-LIAB-005', 'Pattern',       'LOW',      '2026-03-28');

-- 6 SIU investigations
INSERT INTO investigations (claim_id, risk_score_id, status, assigned_to, opened_by, opened_at, closed_at, notes) VALUES
('CLM-2026-PROP-007', 1, 'UNDER_REVIEW', 'priya_fraud',  'priya_fraud', '2026-04-06 10:30:00', NULL,                 'PRV-MIAMI-041 mill suspect — 3 high-risk claims same provider, same billing code BC-7733.'),
('CLM-2026-HLTH-003', 2, 'UNDER_REVIEW', 'priya_fraud',  'priya_fraud', '2026-04-06 10:35:00', NULL,                 'Linked to PRV-MIAMI-041 mill cluster. Cross-reference billing patterns.'),
('CLM-2026-HLTH-007', 3, 'NEW',          NULL,           'priya_fraud', '2026-04-06 10:38:00', NULL,                 'Linked to PRV-MIAMI-041 mill cluster.'),
('CLM-2026-LIAB-006', 4, 'NEW',          NULL,           'priya_fraud', '2026-04-15 09:12:00', NULL,                 'Auto-escalation from risk score 96. New policy + $215K claim.'),
('CLM-2026-WC-004',   5, 'CLOSED',       'priya_fraud',  'priya_fraud', '2026-01-20 11:00:00', '2026-02-15 16:30:00','Closed — denied claim was a duplicate filing. No fraud confirmed.'),
('CLM-2026-AUTO-006', 8, 'CLOSED',       'priya_fraud',  'priya_fraud', '2026-03-25 14:00:00', '2026-04-10 17:00:00','Closed — high cost was legit (luxury vehicle total loss). Dismissed.');

-- ============================================================================
-- 5. DENIAL & LEAKAGE  (denial_leakage: denial_pattern, leakage_flag)
-- ============================================================================
USE denial_leakage;
DELETE FROM leakage_flag;
DELETE FROM denial_pattern;
ALTER TABLE denial_pattern AUTO_INCREMENT = 1;
ALTER TABLE leakage_flag   AUTO_INCREMENT = 1;

INSERT INTO denial_pattern (claim_id, denial_code, reason, occurrence_date) VALUES
('CLM-2026-HLTH-005', 'CO-4',   'Policy lapsed at time of service',         '2026-01-19'),
('CLM-2026-LIAB-003', 'CO-16',  'Claim/service lacks required information', '2026-01-18'),
('CLM-2026-WC-004',   'CO-29',  'Time limit for filing has expired',        '2026-01-15'),
('CLM-2026-HLTH-002', 'CO-4',   'Policy lapsed at time of service',         '2025-09-28'),
('CLM-2026-AUTO-002', 'CO-50',  'Non-covered services',                     '2025-11-20'),
('CLM-2026-PROP-002', 'CO-16',  'Claim/service lacks required information', '2025-10-15'),
('CLM-2026-HLTH-006', 'CO-4',   'Policy lapsed at time of service',         '2026-02-25'),
('CLM-2026-WC-001',   'CO-97',  'Service not separately payable',           '2025-08-28'),
('CLM-2026-LIAB-001', 'CO-16',  'Claim/service lacks required information', '2025-09-20'),
('CLM-2026-AUTO-005', 'CO-50',  'Non-covered services',                     '2026-03-05'),
('CLM-2026-PROP-008', 'CO-29',  'Time limit for filing has expired',        '2026-04-28'),
('CLM-2026-HLTH-008', 'CO-4',   'Policy lapsed at time of service',         '2026-04-15'),  -- CO-4 spike
('CLM-2026-WC-005',   'CO-4',   'Policy lapsed at time of service',         '2026-02-22'),  -- CO-4 spike
('CLM-2026-AUTO-009', 'CO-4',   'Policy lapsed at time of service',         '2026-05-01');  -- CO-4 spike

-- 8 leakage flags
INSERT INTO leakage_flag (claim_id, leakage_type, estimated_loss, identified_date) VALUES
('CLM-2026-PROP-007', 'Overpayment', 18500.00, '2026-04-05'),
('CLM-2026-LIAB-002', 'Overpayment',  8200.00, '2025-11-05'),
('CLM-2026-HLTH-003', 'Overpayment',  4500.00, '2025-11-08'),
('CLM-2026-WC-002',   'Delay',        2300.00, '2025-10-25'),
('CLM-2026-LIAB-006', 'Overpayment', 15000.00, '2026-04-14'),
('CLM-2026-PROP-005', 'Error',         800.00, '2026-03-04'),
('CLM-2026-AUTO-006', 'Delay',        1100.00, '2026-04-01'),
('CLM-2026-HLTH-005', 'Error',         650.00, '2026-01-22');

-- ============================================================================
-- 6. COST / RESERVE / AGING  (claims_cost_reserve_db)
-- ============================================================================
USE claims_cost_reserve_db;
DELETE FROM aging_record;
DELETE FROM claim_reserve;
DELETE FROM claim_cost;
ALTER TABLE claim_cost    AUTO_INCREMENT = 1;
ALTER TABLE claim_reserve AUTO_INCREMENT = 1;
ALTER TABLE aging_record  AUTO_INCREMENT = 1;

-- 110 cost rows (mix of types per claim)
INSERT INTO claim_cost (claim_id, cost_type, amount, cost_date) VALUES
-- AUTO claims — REPAIR + SETTLEMENT
('CLM-2026-AUTO-001', 'REPAIR',     3200.00, '2025-12-22'), ('CLM-2026-AUTO-001', 'SETTLEMENT', 1300.00, '2025-12-29'),
('CLM-2026-AUTO-002', 'REPAIR',    10800.00, '2025-12-05'), ('CLM-2026-AUTO-002', 'SETTLEMENT', 2000.00, '2025-12-12'),
('CLM-2026-AUTO-003', 'REPAIR',     2300.00, '2026-01-19'),
('CLM-2026-AUTO-004', 'REPAIR',    14500.00, '2026-02-25'), ('CLM-2026-AUTO-004', 'SETTLEMENT', 4400.00, '2026-03-04'),
('CLM-2026-AUTO-005', 'REPAIR',      850.00, '2026-03-13'),
('CLM-2026-AUTO-006', 'REPAIR',    38000.00, '2026-04-10'), ('CLM-2026-AUTO-006', 'SETTLEMENT', 9000.00, '2026-04-18'),
('CLM-2026-AUTO-007', 'REPAIR',     6200.00, '2026-04-23'),
('CLM-2026-AUTO-008', 'REPAIR',    12000.00, '2026-05-01'), ('CLM-2026-AUTO-008', 'SETTLEMENT', 3500.00, '2026-05-08'),
('CLM-2026-AUTO-009', 'REPAIR',     3300.00, '2026-05-10'),
('CLM-2026-AUTO-010', 'REPAIR',     8800.00, '2026-05-10'),
-- PROPERTY — REPAIR + LEGAL + SETTLEMENT
('CLM-2026-PROP-001', 'REPAIR',    72000.00, '2025-09-25'), ('CLM-2026-PROP-001', 'LEGAL',  4500.00, '2025-10-01'), ('CLM-2026-PROP-001', 'SETTLEMENT', 8500.00, '2025-10-12'),
('CLM-2026-PROP-002', 'REPAIR',    20000.00, '2025-10-25'), ('CLM-2026-PROP-002', 'SETTLEMENT', 2300.00, '2025-11-05'),
('CLM-2026-PROP-003', 'REPAIR',   145000.00, '2025-11-22'), ('CLM-2026-PROP-003', 'LEGAL', 12000.00, '2025-12-05'), ('CLM-2026-PROP-003', 'SETTLEMENT', 18000.00, '2025-12-10'),
('CLM-2026-PROP-004', 'REPAIR',     9800.00, '2026-02-10'),
('CLM-2026-PROP-005', 'REPAIR',    52000.00, '2026-03-15'), ('CLM-2026-PROP-005', 'LEGAL',  3500.00, '2026-03-22'), ('CLM-2026-PROP-005', 'SETTLEMENT', 7000.00, '2026-03-26'),
('CLM-2026-PROP-006', 'REPAIR',    28000.00, '2026-04-01'), ('CLM-2026-PROP-006', 'SETTLEMENT', 3000.00, '2026-04-07'),
('CLM-2026-PROP-007', 'REPAIR',   210000.00, '2026-04-22'), ('CLM-2026-PROP-007', 'LEGAL', 18000.00, '2026-04-30'), ('CLM-2026-PROP-007', 'SETTLEMENT', 20000.00, '2026-05-06'),
('CLM-2026-PROP-008', 'REPAIR',     4400.00, '2026-05-10'),
('CLM-2026-PROP-009', 'REPAIR',    50000.00, '2026-05-12'), ('CLM-2026-PROP-009', 'SETTLEMENT', 4000.00, '2026-05-12'),
-- HEALTH — MEDICAL only
('CLM-2026-HLTH-001', 'MEDICAL',    3200.00, '2025-08-30'),
('CLM-2026-HLTH-002', 'MEDICAL',   14500.00, '2025-10-17'),
('CLM-2026-HLTH-003', 'MEDICAL',   88000.00, '2025-11-25'), ('CLM-2026-HLTH-003', 'LEGAL', 5000.00, '2025-12-01'), ('CLM-2026-HLTH-003', 'SETTLEMENT', 5000.00, '2025-12-01'),
('CLM-2026-HLTH-004', 'MEDICAL',    7200.00, '2025-12-26'),
('CLM-2026-HLTH-005', 'MEDICAL',   42000.00, '2026-02-18'),
('CLM-2026-HLTH-006', 'MEDICAL',    2800.00, '2026-03-08'),
('CLM-2026-HLTH-007', 'MEDICAL',   60000.00, '2026-04-04'), ('CLM-2026-HLTH-007', 'LEGAL', 4000.00, '2026-04-04'), ('CLM-2026-HLTH-007', 'SETTLEMENT', 3500.00, '2026-04-04'),
('CLM-2026-HLTH-008', 'MEDICAL',   11200.00, '2026-05-02'),
-- LIABILITY — LEGAL + SETTLEMENT
('CLM-2026-LIAB-001', 'LEGAL',      8000.00, '2025-10-08'), ('CLM-2026-LIAB-001', 'SETTLEMENT', 27000.00, '2025-10-15'),
('CLM-2026-LIAB-002', 'LEGAL',     22000.00, '2025-11-25'), ('CLM-2026-LIAB-002', 'SETTLEMENT', 98000.00, '2025-12-01'),
('CLM-2026-LIAB-003', 'LEGAL',     11000.00, '2026-02-07'),
('CLM-2026-LIAB-004', 'LEGAL',     14000.00, '2026-02-28'), ('CLM-2026-LIAB-004', 'SETTLEMENT', 78000.00, '2026-03-07'),
('CLM-2026-LIAB-005', 'LEGAL',      1500.00, '2026-04-09'), ('CLM-2026-LIAB-005', 'SETTLEMENT', 4300.00, '2026-04-09'),
('CLM-2026-LIAB-006', 'LEGAL',     35000.00, '2026-05-05'), ('CLM-2026-LIAB-006', 'SETTLEMENT', 180000.00, '2026-05-12'),
-- WORKERS COMP — MEDICAL + SETTLEMENT
('CLM-2026-WC-001', 'MEDICAL', 9000.00, '2025-09-10'), ('CLM-2026-WC-001', 'SETTLEMENT',  3000.00, '2025-09-15'),
('CLM-2026-WC-002', 'MEDICAL',38000.00, '2025-11-04'), ('CLM-2026-WC-002', 'SETTLEMENT', 10500.00, '2025-11-11'),
('CLM-2026-WC-003', 'MEDICAL', 7500.00, '2025-12-12'),
('CLM-2026-WC-004', 'MEDICAL',88000.00, '2026-02-08'), ('CLM-2026-WC-004', 'LEGAL',  6000.00, '2026-02-12'), ('CLM-2026-WC-004', 'SETTLEMENT', 11000.00, '2026-02-15'),
('CLM-2026-WC-005', 'MEDICAL', 3400.00, '2026-03-04'),
('CLM-2026-WC-006', 'MEDICAL',23000.00, '2026-03-30'), ('CLM-2026-WC-006', 'SETTLEMENT', 6000.00, '2026-04-04'),
('CLM-2026-WC-007', 'MEDICAL',16500.00, '2026-05-04'), ('CLM-2026-WC-007', 'SETTLEMENT', 2000.00, '2026-05-10');

-- One reserve per claim — 120% of expected payout
INSERT INTO claim_reserve (claim_id, reserve_amount, updated_date) VALUES
('CLM-2026-AUTO-001', 5400.00, '2025-12-15'),    ('CLM-2026-AUTO-002', 15360.00, '2025-11-20'),
('CLM-2026-AUTO-003', 2760.00, '2026-01-08'),    ('CLM-2026-AUTO-004', 22680.00, '2026-02-14'),
('CLM-2026-AUTO-005', 1020.00, '2026-03-05'),    ('CLM-2026-AUTO-006', 56400.00, '2026-03-22'),
('CLM-2026-AUTO-007', 7440.00, '2026-04-10'),    ('CLM-2026-AUTO-008', 18600.00, '2026-04-22'),
('CLM-2026-AUTO-009', 3960.00, '2026-05-01'),    ('CLM-2026-AUTO-010', 10560.00, '2026-05-03'),
('CLM-2026-PROP-001', 102000.00, '2025-09-10'),  ('CLM-2026-PROP-002', 26760.00, '2025-10-15'),
('CLM-2026-PROP-003', 210000.00, '2025-11-12'),  ('CLM-2026-PROP-004', 11760.00, '2026-01-26'),
('CLM-2026-PROP-005', 75000.00, '2026-03-02'),   ('CLM-2026-PROP-006', 37200.00, '2026-03-19'),
('CLM-2026-PROP-007', 297600.00, '2026-04-05'),  ('CLM-2026-PROP-008', 5280.00, '2026-04-28'),
('CLM-2026-PROP-009', 64800.00, '2026-05-04'),
('CLM-2026-HLTH-001', 3840.00, '2025-08-14'),    ('CLM-2026-HLTH-002', 17400.00, '2025-09-28'),
('CLM-2026-HLTH-003', 117600.00, '2025-11-05'),  ('CLM-2026-HLTH-004', 8640.00, '2025-12-12'),
('CLM-2026-HLTH-005', 50400.00, '2026-01-19'),   ('CLM-2026-HLTH-006', 3360.00, '2026-02-25'),
('CLM-2026-HLTH-007', 81000.00, '2026-03-12'),   ('CLM-2026-HLTH-008', 13440.00, '2026-04-15'),
('CLM-2026-LIAB-001', 42000.00, '2025-09-20'),   ('CLM-2026-LIAB-002', 144000.00, '2025-11-02'),
('CLM-2026-LIAB-003', 81600.00, '2026-01-18'),   ('CLM-2026-LIAB-004', 110400.00, '2026-02-12'),
('CLM-2026-LIAB-005', 6960.00, '2026-03-28'),    ('CLM-2026-LIAB-006', 258000.00, '2026-04-14'),
('CLM-2026-WC-001', 14400.00, '2025-08-28'),     ('CLM-2026-WC-002', 58200.00, '2025-10-18'),
('CLM-2026-WC-003', 9000.00, '2025-11-29'),      ('CLM-2026-WC-004', 126000.00, '2026-01-15'),
('CLM-2026-WC-005', 4080.00, '2026-02-22'),      ('CLM-2026-WC-006', 34800.00, '2026-03-16'),
('CLM-2026-WC-007', 22200.00, '2026-04-24');

-- Aging records — bucket based on days since filed (computed against 2026-05-05)
INSERT INTO aging_record (claim_id, aging_days, aging_bucket) VALUES
('CLM-2026-AUTO-001', 141, 'BUCKET_90_PLUS'),  ('CLM-2026-AUTO-002', 166, 'BUCKET_90_PLUS'),
('CLM-2026-AUTO-003', 117, 'BUCKET_90_PLUS'),  ('CLM-2026-AUTO-004', 80,  'BUCKET_61_90'),
('CLM-2026-AUTO-005', 61,  'BUCKET_31_60'),    ('CLM-2026-AUTO-006', 44,  'BUCKET_31_60'),
('CLM-2026-AUTO-007', 25,  'BUCKET_0_30'),     ('CLM-2026-AUTO-008', 13,  'BUCKET_0_30'),
('CLM-2026-AUTO-009', 4,   'BUCKET_0_30'),     ('CLM-2026-AUTO-010', 2,   'BUCKET_0_30'),
('CLM-2026-PROP-001', 237, 'BUCKET_90_PLUS'),  ('CLM-2026-PROP-002', 202, 'BUCKET_90_PLUS'),
('CLM-2026-PROP-003', 174, 'BUCKET_90_PLUS'),  ('CLM-2026-PROP-004', 99,  'BUCKET_90_PLUS'),
('CLM-2026-PROP-005', 64,  'BUCKET_61_90'),    ('CLM-2026-PROP-006', 47,  'BUCKET_31_60'),
('CLM-2026-PROP-007', 30,  'BUCKET_0_30'),     ('CLM-2026-PROP-008', 7,   'BUCKET_0_30'),
('CLM-2026-PROP-009', 1,   'BUCKET_0_30'),
('CLM-2026-HLTH-001', 264, 'BUCKET_90_PLUS'),  ('CLM-2026-HLTH-002', 220, 'BUCKET_90_PLUS'),
('CLM-2026-HLTH-003', 181, 'BUCKET_90_PLUS'),  ('CLM-2026-HLTH-004', 144, 'BUCKET_90_PLUS'),
('CLM-2026-HLTH-005', 106, 'BUCKET_90_PLUS'),  ('CLM-2026-HLTH-006', 69,  'BUCKET_61_90'),
('CLM-2026-HLTH-007', 54,  'BUCKET_31_60'),    ('CLM-2026-HLTH-008', 20,  'BUCKET_0_30'),
('CLM-2026-LIAB-001', 227, 'BUCKET_90_PLUS'),  ('CLM-2026-LIAB-002', 184, 'BUCKET_90_PLUS'),
('CLM-2026-LIAB-003', 107, 'BUCKET_90_PLUS'),  ('CLM-2026-LIAB-004', 82,  'BUCKET_61_90'),
('CLM-2026-LIAB-005', 38,  'BUCKET_31_60'),    ('CLM-2026-LIAB-006', 21,  'BUCKET_0_30'),
('CLM-2026-WC-001',   250, 'BUCKET_90_PLUS'),  ('CLM-2026-WC-002',   199, 'BUCKET_90_PLUS'),
('CLM-2026-WC-003',   158, 'BUCKET_90_PLUS'),  ('CLM-2026-WC-004',   110, 'BUCKET_90_PLUS'),
('CLM-2026-WC-005',   72,  'BUCKET_61_90'),    ('CLM-2026-WC-006',   50,  'BUCKET_31_60'),
('CLM-2026-WC-007',   11,  'BUCKET_0_30');

-- ============================================================================
-- 7. ADJUSTER PERFORMANCE  (AdjusterPerformanceDB)
-- ============================================================================
USE AdjusterPerformanceDB;
DELETE FROM sla_violation;
DELETE FROM adjuster_performance;
ALTER TABLE adjuster_performance AUTO_INCREMENT = 1;
ALTER TABLE sla_violation        AUTO_INCREMENT = 1;

-- 8 adjusters × 4 quarters = 32 rows
INSERT INTO adjuster_performance (adjuster_id, claims_handled, total_days_taken, avg_tat, quality_score, sla_met_count, sla_breached_count, denied_claims_count, error_rate, period) VALUES
-- Q3-2025
(1, 24, 408, 17.0, 88.5, 21, 3,  2, 1.8, 'Q3-2025'),
(2, 31, 558, 18.0, 91.2, 28, 3,  3, 1.2, 'Q3-2025'),
(3, 19, 304, 16.0, 85.0, 16, 3,  4, 3.5, 'Q3-2025'),
(4, 22, 396, 18.0, 87.3, 19, 3,  2, 2.1, 'Q3-2025'),
(5, 18, 270, 15.0, 92.1, 17, 1,  1, 0.8, 'Q3-2025'),
(6, 28, 532, 19.0, 79.5, 22, 6,  5, 4.2, 'Q3-2025'),
(7, 21, 357, 17.0, 86.8, 18, 3,  3, 2.5, 'Q3-2025'),
(8, 16, 256, 16.0, 89.0, 14, 2,  1, 1.5, 'Q3-2025'),
-- Q4-2025
(1, 26, 442, 17.0, 89.0, 23, 3,  3, 1.5, 'Q4-2025'),
(2, 33, 627, 19.0, 90.5, 29, 4,  3, 1.4, 'Q4-2025'),
(3, 21, 357, 17.0, 84.2, 17, 4,  4, 3.8, 'Q4-2025'),
(4, 20, 380, 19.0, 86.0, 17, 3,  2, 2.4, 'Q4-2025'),
(5, 19, 285, 15.0, 93.5, 18, 1,  1, 0.5, 'Q4-2025'),
(6, 30, 600, 20.0, 76.8, 22, 8,  6, 5.1, 'Q4-2025'),
(7, 23, 414, 18.0, 87.5, 20, 3,  2, 2.0, 'Q4-2025'),
(8, 17, 272, 16.0, 90.2, 15, 2,  1, 1.2, 'Q4-2025'),
-- Q1-2026
(1, 28, 476, 17.0, 90.1, 25, 3,  2, 1.3, 'Q1-2026'),
(2, 35, 700, 20.0, 89.8, 30, 5,  4, 1.6, 'Q1-2026'),
(3, 23, 437, 19.0, 83.5, 18, 5,  5, 4.2, 'Q1-2026'),
(4, 24, 480, 20.0, 85.4, 20, 4,  3, 2.8, 'Q1-2026'),
(5, 21, 315, 15.0, 94.2, 20, 1,  1, 0.4, 'Q1-2026'),
(6, 32, 704, 22.0, 74.2, 22, 10, 8, 6.5, 'Q1-2026'),
(7, 25, 475, 19.0, 88.0, 22, 3,  2, 1.8, 'Q1-2026'),
(8, 19, 304, 16.0, 91.5, 17, 2,  1, 1.0, 'Q1-2026'),
-- Q2-2026 (partial — through May 5)
(1, 12, 192, 16.0, 91.0, 11, 1, 1, 1.2, 'Q2-2026'),
(2, 14, 252, 18.0, 90.0, 12, 2, 1, 1.5, 'Q2-2026'),
(3, 10, 180, 18.0, 84.0, 8,  2, 2, 4.0, 'Q2-2026'),
(4, 11, 198, 18.0, 86.5, 9,  2, 1, 2.3, 'Q2-2026'),
(5,  9, 126, 14.0, 95.0, 9,  0, 0, 0.3, 'Q2-2026'),
(6, 13, 260, 20.0, 73.0, 9,  4, 3, 6.8, 'Q2-2026'),
(7, 11, 198, 18.0, 88.5, 10, 1, 1, 1.5, 'Q2-2026'),
(8,  8, 128, 16.0, 92.0, 7,  1, 0, 0.8, 'Q2-2026');

-- 18 SLA violations (claim_id stored as Long because of SLAViolation entity quirk)
INSERT INTO sla_violation (claim_id, adjuster_id, violation_type, sla_target_days, actual_days, violation_date) VALUES
(1,  6, 'TAT_EXCEEDED',         15, 32, '2025-10-12'),
(2,  6, 'CYCLE_TIME_EXCEEDED',  20, 41, '2025-10-12'),
(3,  3, 'TAT_EXCEEDED',         15, 28, '2025-12-10'),
(4,  6, 'SETTLEMENT_DELAYED',   30, 47, '2025-10-15'),
(5,  3, 'TAT_EXCEEDED',         15, 30, '2026-02-18'),
(6,  6, 'CYCLE_TIME_EXCEEDED',  20, 38, '2026-02-18'),
(7,  3, 'SETTLEMENT_DELAYED',   30, 46, '2026-02-18'),
(8,  1, 'TAT_EXCEEDED',         15, 31, '2026-05-06'),
(9,  3, 'CYCLE_TIME_EXCEEDED',  20, 42, '2026-05-06'),
(10, 1, 'SETTLEMENT_DELAYED',   30, 50, '2026-05-06'),
(11, 6, 'TAT_EXCEEDED',         15, 27, '2026-04-18'),
(12, 1, 'TAT_EXCEEDED',         15, 31, '2026-02-15'),
(13, 1, 'CYCLE_TIME_EXCEEDED',  20, 40, '2026-02-15'),
(14, 1, 'SETTLEMENT_DELAYED',   30, 48, '2026-02-15'),
(15, 7, 'TAT_EXCEEDED',         15, 28, '2026-05-12'),
(16, 7, 'CYCLE_TIME_EXCEEDED',  20, 36, '2026-05-12'),
(17, 7, 'SETTLEMENT_DELAYED',   30, 43, '2026-05-12'),
(18, 4, 'TAT_EXCEEDED',         15, 25, '2025-09-20');

-- ============================================================================
-- 8. NOTIFICATIONS  (NotificationsDB: users, notifications)
-- ============================================================================
USE NotificationsDB;
DELETE FROM notifications;
DELETE FROM users;
ALTER TABLE users         AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;

-- Mirror users (NotificationService side — UserRole enum is shorter/different)
INSERT INTO users (user_id, name, email, role, is_active) VALUES
(1, 'Alice Admin',   'alice.admin@acme-carrier.com',  'ADMIN',     1),
(2, 'Sarah Chen',    'sarah.chen@acme-carrier.com',   'ANALYST',   1),
(3, 'David Patel',   'david.patel@acme-carrier.com',  'MANAGER',   1),
(4, 'Priya Menon',   'priya.menon@acme-carrier.com',  'FRAUD',     1),
(5, 'Nadia Rashid',  'nadia.rashid@acme-carrier.com', 'ACTUARY',   1),
(6, 'Lena Garcia',   'lena.garcia@acme-carrier.com',  'EXECUTIVE', 1);

-- 95 notifications across all users — mix of UNREAD/READ/DISMISSED
INSERT INTO notifications (user_id, title, message, category, reference_id, status, created_date, read_date) VALUES
-- RISK alerts to Priya (Fraud Analyst)
(4, 'High-risk claim flagged',         'Claim CLM-2026-PROP-007 scored 94 — provider PRV-MIAMI-041 mill suspect.', 'RISK', 'CLM-2026-PROP-007', 'UNREAD',    '2026-04-05 06:08:00', NULL),
(4, 'High-risk claim flagged',         'Claim CLM-2026-LIAB-006 scored 96 — $215K + new policy.',                  'RISK', 'CLM-2026-LIAB-006', 'UNREAD',    '2026-04-14 06:08:00', NULL),
(4, 'High-risk claim flagged',         'Claim CLM-2026-WC-004 scored 90 — $105K denied claim.',                    'RISK', 'CLM-2026-WC-004',   'READ',      '2026-01-15 06:08:00', '2026-01-15 09:30:00'),
(4, 'High-risk claim flagged',         'Claim CLM-2026-PROP-003 scored 93 — new policy + $175K.',                  'RISK', 'CLM-2026-PROP-003', 'READ',      '2025-11-12 06:08:00', '2025-11-12 11:00:00'),
(4, 'High-risk claim flagged',         'Claim CLM-2026-AUTO-006 scored 95.',                                       'RISK', 'CLM-2026-AUTO-006', 'DISMISSED', '2026-03-22 06:08:00', '2026-03-25 14:30:00'),
(4, 'New SIU escalation',              'priya_fraud escalated CLM-2026-PROP-007 for SIU review.',                  'RISK', 'CLM-2026-PROP-007', 'READ',      '2026-04-06 10:30:00', '2026-04-06 10:35:00'),
(4, 'New SIU escalation',              'priya_fraud escalated CLM-2026-LIAB-006 for SIU review.',                  'RISK', 'CLM-2026-LIAB-006', 'UNREAD',    '2026-04-15 09:12:00', NULL),
-- RISK alerts also broadcast to Admin
(1, 'High-risk claim flagged',         'Claim CLM-2026-PROP-007 scored 94 — provider PRV-MIAMI-041 mill suspect.', 'RISK', 'CLM-2026-PROP-007', 'READ',      '2026-04-05 06:08:00', '2026-04-05 09:00:00'),
(1, 'High-risk claim flagged',         'Claim CLM-2026-LIAB-006 scored 96 — $215K + new policy.',                  'RISK', 'CLM-2026-LIAB-006', 'UNREAD',    '2026-04-14 06:08:00', NULL),
(1, 'New SIU escalation',              'priya_fraud escalated CLM-2026-PROP-007 for SIU review.',                  'RISK', 'CLM-2026-PROP-007', 'READ',      '2026-04-06 10:30:00', '2026-04-06 11:00:00'),
(1, 'New SIU escalation',              'priya_fraud escalated CLM-2026-LIAB-006 for SIU review.',                  'RISK', 'CLM-2026-LIAB-006', 'UNREAD',    '2026-04-15 09:12:00', NULL),
-- DENIAL alerts to Sarah (Claims Analyst)
(2, 'Denial spike detected',           'CO-4 (policy lapsed) doubled this week — 4 occurrences.',                  'DENIAL', NULL,                'UNREAD',    '2026-05-01 08:00:00', NULL),
(2, 'New denial pattern',              'CLM-2026-HLTH-005 denied: CO-4 policy lapsed.',                            'DENIAL', 'CLM-2026-HLTH-005', 'READ',      '2026-01-19 06:10:00', '2026-01-19 09:15:00'),
(2, 'New denial pattern',              'CLM-2026-LIAB-003 denied: CO-16 missing information.',                     'DENIAL', 'CLM-2026-LIAB-003', 'READ',      '2026-01-18 06:10:00', '2026-01-18 10:00:00'),
(2, 'New denial pattern',              'CLM-2026-WC-004 denied: CO-29 time limit expired.',                        'DENIAL', 'CLM-2026-WC-004',   'READ',      '2026-01-15 06:10:00', '2026-01-15 11:00:00'),
(2, 'Leakage flag raised',             'Overpayment detected on CLM-2026-PROP-007: $18,500 estimated loss.',       'DENIAL', 'CLM-2026-PROP-007', 'UNREAD',    '2026-04-05 06:12:00', NULL),
(2, 'Leakage flag raised',             'Overpayment on CLM-2026-LIAB-002: $8,200.',                                'DENIAL', 'CLM-2026-LIAB-002', 'READ',      '2025-11-05 06:12:00', '2025-11-05 14:00:00'),
-- COST/AGING alerts to Nadia (Actuary)
(5, 'Reserve adequacy warning',        'CLM-2026-PROP-007 reserve $297K vs paid $248K — adequate ratio 120%.',     'COST',  'CLM-2026-PROP-007', 'UNREAD',    '2026-05-02 06:00:00', NULL),
(5, 'Severity trend alert',            'Bodily injury severity grew 12% YoY against 7% pricing assumption.',       'COST',  NULL,                'UNREAD',    '2026-05-03 06:00:00', NULL),
(5, 'Aging — claim 90+',               '13 claims now in 90+ aging bucket.',                                       'AGING', NULL,                'READ',      '2026-05-04 06:00:00', '2026-05-04 09:00:00'),
(5, 'Loss ratio escalation',           'Commercial property segment loss ratio climbed 62% → 71% in 2 months.',    'COST',  NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
(5, 'Aging — claim 90+',               '12 claims now in 90+ aging bucket.',                                       'AGING', NULL,                'READ',      '2026-04-15 06:00:00', '2026-04-15 14:00:00'),
-- PERFORMANCE alerts to David (Manager)
(3, 'Adjuster performance alert',      'Adjuster #6 (Tom) error rate hit 6.5% in Q1-2026 — flagged for training.', 'PERFORMANCE', NULL,           'UNREAD',    '2026-04-01 08:00:00', NULL),
(3, 'SLA breach',                      'CLM-2026-PROP-007 breached TAT SLA by 16 days.',                           'PERFORMANCE', 'CLM-2026-PROP-007', 'READ',  '2026-05-06 06:15:00', '2026-05-06 11:00:00'),
(3, 'SLA breach',                      'CLM-2026-WC-004 breached TAT SLA by 16 days.',                             'PERFORMANCE', 'CLM-2026-WC-004',   'READ',  '2026-02-15 06:15:00', '2026-02-15 09:30:00'),
(3, 'SLA breach',                      'CLM-2026-LIAB-006 breached SLA by 13 days.',                               'PERFORMANCE', 'CLM-2026-LIAB-006', 'UNREAD','2026-05-12 06:15:00', NULL),
(3, 'Top performer',                   'Adjuster #5 quality score 94.2 in Q1-2026 — recognise & reward.',          'PERFORMANCE', NULL,           'READ',      '2026-04-01 08:00:00', '2026-04-01 14:00:00'),
(3, 'Adjuster overload',               'Adjuster #2 handling 35 claims in Q1 (avg 22).',                           'PERFORMANCE', NULL,           'UNREAD',    '2026-04-15 08:00:00', NULL),
-- Operations Exec
(6, 'Loss ratio breach',               'Commercial property loss ratio climbed to 71%.',                           'COST',  NULL,                'UNREAD',    '2026-05-03 06:00:00', NULL),
(6, 'Settlement rate dropped',         'Q1-2026 settlement rate 73% vs target 80%.',                               'PERFORMANCE', NULL,           'READ',      '2026-04-05 06:00:00', '2026-04-05 10:00:00'),
(6, 'SLA compliance summary',          '85% TAT compliance overall — below 90% target.',                           'PERFORMANCE', NULL,           'UNREAD',    '2026-05-01 06:00:00', NULL),
(6, 'Aging — 90+ alert',               '13 claims aged > 90 days.',                                                'AGING', NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
-- Sarah more
(2, 'Cost spike detected',             'Medical costs up 18% MoM.',                                                'COST',  NULL,                'READ',      '2026-04-30 06:00:00', '2026-04-30 09:00:00'),
(2, 'Denial spike — CO-4',             '3 CO-4 denials in last 7 days.',                                           'DENIAL', NULL,               'UNREAD',    '2026-05-04 06:10:00', NULL),
-- Admin (system + summary)
(1, 'New user registered',             'sarah_analyst registered with role ROLE_CLAIMS_ANALYST.',                   'SYSTEM', NULL,                'READ',      '2025-02-01 10:30:00', '2025-02-01 11:00:00'),
(1, 'New user registered',             'david_manager registered with role ROLE_CLAIMS_MANAGER.',                   'SYSTEM', NULL,                'READ',      '2025-02-05 11:00:00', '2025-02-05 14:00:00'),
(1, 'New user registered',             'priya_fraud registered with role ROLE_FRAUD_ANALYST.',                      'SYSTEM', NULL,                'READ',      '2025-02-10 09:15:00', '2025-02-10 10:00:00'),
(1, 'New user registered',             'nadia_actuary registered with role ROLE_ACTUARY.',                          'SYSTEM', NULL,                'READ',      '2025-02-12 14:00:00', '2025-02-12 15:00:00'),
(1, 'New user registered',             'lena_ops registered with role ROLE_OPERATIONS_EXEC.',                       'SYSTEM', NULL,                'READ',      '2025-02-15 09:30:00', '2025-02-15 10:30:00'),
(1, 'Data feed sync failure',          'Feed #5 (Legacy COBOL Mainframe) sync FAILED — last successful 2025-11-20.','SYSTEM', NULL,                'UNREAD',    '2025-11-21 06:00:00', NULL),
-- More distributed alerts
(2, 'New claim ingested',              'Claim CLM-2026-AUTO-010 ingested from GuideWire.',                         'SYSTEM', 'CLM-2026-AUTO-010', 'READ',      '2026-05-03 06:05:00', '2026-05-03 09:30:00'),
(2, 'New claim ingested',              'Claim CLM-2026-PROP-009 ingested.',                                        'SYSTEM', 'CLM-2026-PROP-009', 'UNREAD',    '2026-05-04 06:05:00', NULL),
(3, 'Cycle time degradation',          'Avg cycle time increased to 26 days (target 20).',                         'PERFORMANCE', NULL,           'UNREAD',    '2026-05-01 06:00:00', NULL),
(4, 'Pattern indicator triggered',     'CLM-2026-HLTH-007 — provider repeat (PRV-MIAMI-041).',                     'RISK', 'CLM-2026-HLTH-007', 'READ',      '2026-03-12 06:08:00', '2026-03-12 11:30:00'),
(4, 'New SIU escalation',              'priya_fraud escalated CLM-2026-HLTH-003 for SIU review.',                  'RISK', 'CLM-2026-HLTH-003', 'READ',      '2026-04-06 10:35:00', '2026-04-06 10:40:00'),
(4, 'New SIU escalation',              'priya_fraud escalated CLM-2026-HLTH-007 for SIU review.',                  'RISK', 'CLM-2026-HLTH-007', 'UNREAD',    '2026-04-06 10:38:00', NULL),
(1, 'New SIU escalation',              'priya_fraud escalated CLM-2026-HLTH-003 for SIU review.',                  'RISK', 'CLM-2026-HLTH-003', 'READ',      '2026-04-06 10:35:00', '2026-04-06 12:00:00'),
(1, 'New SIU escalation',              'priya_fraud escalated CLM-2026-HLTH-007 for SIU review.',                  'RISK', 'CLM-2026-HLTH-007', 'UNREAD',    '2026-04-06 10:38:00', NULL),
-- Additional generic alerts to populate the feed
(2, 'Reserve change',                  'Reserve on CLM-2026-LIAB-006 increased to $258K.',                         'COST',  'CLM-2026-LIAB-006', 'READ',      '2026-04-14 14:00:00', '2026-04-14 15:00:00'),
(3, 'SLA approaching',                 'CLM-2026-PROP-007 approaching SLA target (28/30 days).',                   'PERFORMANCE', 'CLM-2026-PROP-007', 'READ',  '2026-05-05 06:00:00', '2026-05-05 09:30:00'),
(5, 'Reserve change',                  'Reserve on CLM-2026-PROP-007 set to $297K.',                               'COST',  'CLM-2026-PROP-007', 'READ',      '2026-04-05 14:00:00', '2026-04-05 16:00:00'),
(6, 'Quarterly summary',               'Q1-2026 portfolio: 28 claims closed, $480K paid, 85% SLA compliance.',     'PERFORMANCE', NULL,           'READ',      '2026-04-01 06:00:00', '2026-04-01 09:00:00'),
(6, 'Quarterly summary',               'Q4-2025 portfolio: 31 claims closed, $620K paid, 88% SLA compliance.',     'PERFORMANCE', NULL,           'READ',      '2026-01-01 06:00:00', '2026-01-02 09:00:00'),
(2, 'Aging escalation',                '4 claims aged > 90 days under your portfolio.',                            'AGING', NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
(3, 'Aging escalation',                '6 claims under your team aged > 90 days.',                                 'AGING', NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
(4, 'Investigation closed',            'Investigation #5 on CLM-2026-WC-004 closed — no fraud confirmed.',         'RISK', 'CLM-2026-WC-004',   'READ',      '2026-02-15 16:30:00', '2026-02-15 17:00:00'),
(4, 'Investigation closed',            'Investigation #6 on CLM-2026-AUTO-006 closed — dismissed.',                'RISK', 'CLM-2026-AUTO-006', 'READ',      '2026-04-10 17:00:00', '2026-04-10 17:30:00'),
(1, 'Investigation closed',            'Investigation #5 on CLM-2026-WC-004 closed.',                              'RISK', 'CLM-2026-WC-004',   'READ',      '2026-02-15 16:30:00', '2026-02-15 18:00:00'),
(1, 'Investigation closed',            'Investigation #6 on CLM-2026-AUTO-006 closed.',                            'RISK', 'CLM-2026-AUTO-006', 'READ',      '2026-04-10 17:00:00', '2026-04-10 18:00:00'),
(2, 'Cost alert',                      'Settlement amount on CLM-2026-LIAB-006 exceeds $180K.',                    'COST',  'CLM-2026-LIAB-006', 'UNREAD',    '2026-05-12 14:00:00', NULL),
(5, 'Trend alert',                     'Auto severity trending up 8% MoM in Q2-2026.',                             'COST',  NULL,                'UNREAD',    '2026-05-03 06:00:00', NULL),
(6, 'Top adjuster alert',              'Adjuster #5 leading on quality (94.2). Adjuster #6 at 73 — needs coaching.','PERFORMANCE', NULL,          'UNREAD',    '2026-05-01 06:00:00', NULL);

-- ── Admin shadow copies — admin sees a copy of every meaningful event ──────
-- Per the notification policy, ADMIN is part of every targetRoles set so the
-- admin's bell reflects every signal in the system. The seed already gives
-- admin copies of risk + escalation + system events; this block fills in
-- the gaps for DENIAL / PERFORMANCE / AGING / COST that previously only
-- went to the role-specific user.
INSERT INTO notifications (user_id, title, message, category, reference_id, status, created_date, read_date) VALUES
-- DENIAL category copies (Sarah was the only recipient before)
(1, 'Denial spike detected',           'CO-4 (policy lapsed) doubled this week — 4 occurrences.',                 'DENIAL', NULL,                'UNREAD',    '2026-05-01 08:00:00', NULL),
(1, 'New denial pattern',              'CLM-2026-HLTH-005 denied: CO-4 policy lapsed.',                            'DENIAL', 'CLM-2026-HLTH-005', 'READ',      '2026-01-19 06:10:00', '2026-01-19 14:00:00'),
(1, 'New denial pattern',              'CLM-2026-LIAB-003 denied: CO-16 missing information.',                     'DENIAL', 'CLM-2026-LIAB-003', 'READ',      '2026-01-18 06:10:00', '2026-01-18 14:00:00'),
(1, 'Leakage flag raised',             'Overpayment detected on CLM-2026-PROP-007: $18,500 estimated loss.',       'DENIAL', 'CLM-2026-PROP-007', 'UNREAD',    '2026-04-05 06:12:00', NULL),
(1, 'Leakage flag raised',             'Overpayment on CLM-2026-LIAB-002: $8,200.',                                'DENIAL', 'CLM-2026-LIAB-002', 'READ',      '2025-11-05 06:12:00', '2025-11-05 16:00:00'),
(1, 'Denial spike — CO-4',             '3 CO-4 denials in last 7 days.',                                           'DENIAL', NULL,                'UNREAD',    '2026-05-04 06:10:00', NULL),
-- PERFORMANCE copies (David was the only recipient before)
(1, 'Adjuster performance alert',      'Adjuster #6 (Tom) error rate hit 6.5% in Q1-2026 — flagged for training.', 'PERFORMANCE', NULL,           'READ',      '2026-04-01 08:00:00', '2026-04-01 12:00:00'),
(1, 'SLA breach',                      'CLM-2026-PROP-007 breached TAT SLA by 16 days.',                           'PERFORMANCE', 'CLM-2026-PROP-007', 'READ',  '2026-05-06 06:15:00', '2026-05-06 13:00:00'),
(1, 'SLA breach',                      'CLM-2026-WC-004 breached TAT SLA by 16 days.',                             'PERFORMANCE', 'CLM-2026-WC-004',   'READ',  '2026-02-15 06:15:00', '2026-02-15 14:00:00'),
(1, 'SLA breach',                      'CLM-2026-LIAB-006 breached SLA by 13 days.',                               'PERFORMANCE', 'CLM-2026-LIAB-006', 'UNREAD','2026-05-12 06:15:00', NULL),
(1, 'Adjuster overload',               'Adjuster #2 handling 35 claims in Q1 (avg 22).',                           'PERFORMANCE', NULL,           'UNREAD',    '2026-04-15 08:00:00', NULL),
(1, 'Cycle time degradation',          'Avg cycle time increased to 26 days (target 20).',                         'PERFORMANCE', NULL,           'UNREAD',    '2026-05-01 06:00:00', NULL),
-- AGING copies (Nadia / Sarah / David were recipients before)
(1, 'Aging — claim 90+',               '13 claims now in 90+ aging bucket.',                                       'AGING', NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
(1, 'Aging — claim 90+',               '12 claims now in 90+ aging bucket.',                                       'AGING', NULL,                'READ',      '2026-04-15 06:00:00', '2026-04-15 16:00:00'),
(1, 'Aging escalation',                '6 claims under your team aged > 90 days.',                                 'AGING', NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
-- COST copies (Nadia / Lena were recipients before)
(1, 'Reserve adequacy warning',        'CLM-2026-PROP-007 reserve $297K vs paid $248K — adequate ratio 120%.',     'COST',  'CLM-2026-PROP-007', 'UNREAD',    '2026-05-02 06:00:00', NULL),
(1, 'Severity trend alert',            'Bodily injury severity grew 12% YoY against 7% pricing assumption.',       'COST',  NULL,                'UNREAD',    '2026-05-03 06:00:00', NULL),
(1, 'Loss ratio escalation',           'Commercial property segment loss ratio climbed 62% → 71% in 2 months.',    'COST',  NULL,                'UNREAD',    '2026-05-04 06:00:00', NULL),
(1, 'Reserve change',                  'Reserve on CLM-2026-LIAB-006 increased to $258K.',                         'COST',  'CLM-2026-LIAB-006', 'READ',      '2026-04-14 14:00:00', '2026-04-14 17:00:00'),
(1, 'Cost spike detected',             'Medical costs up 18% MoM.',                                                'COST',  NULL,                'READ',      '2026-04-30 06:00:00', '2026-04-30 12:00:00');

-- ============================================================================
-- 9. ANALYTICS REPORTS  (claims_analytics_report_db)
-- ============================================================================
USE claims_analytics_report_db;
DELETE FROM analytics_report;
ALTER TABLE analytics_report AUTO_INCREMENT = 1;

INSERT INTO analytics_report (scope, scope_value, metrics, generated_date, generated_by, report_data) VALUES
('PRODUCT',    'AUTO',           'TAT,SEVERITY,LOSS_RATIO',           '2026-05-04', 'lena_ops',      '{"avgTat":15.3,"severity":12450,"lossRatio":68.4,"claimCount":12}'),
('PRODUCT',    'PROPERTY',       'TAT,SEVERITY,LOSS_RATIO',           '2026-05-04', 'lena_ops',      '{"avgTat":21.4,"severity":76300,"lossRatio":75.2,"claimCount":9}'),
('PRODUCT',    'HEALTH',         'TAT,SEVERITY,LOSS_RATIO',           '2026-05-04', 'lena_ops',      '{"avgTat":19.5,"severity":30425,"lossRatio":73.1,"claimCount":8}'),
('PRODUCT',    'LIABILITY',      'TAT,SEVERITY,LOSS_RATIO',           '2026-05-04', 'lena_ops',      '{"avgTat":22.8,"severity":89300,"lossRatio":77.7,"claimCount":6}'),
('PRODUCT',    'WC',             'TAT,SEVERITY,LOSS_RATIO',           '2026-05-04', 'lena_ops',      '{"avgTat":18.7,"severity":31985,"lossRatio":71.7,"claimCount":7}'),
('REGION',     'NORTHEAST',      'TAT,LOSS_RATIO,FREQUENCY',          '2026-05-01', 'nadia_actuary', '{"avgTat":17.2,"lossRatio":69.8,"frequency":1.4,"claimCount":11}'),
('REGION',     'SOUTHEAST',      'TAT,LOSS_RATIO,FREQUENCY',          '2026-05-01', 'nadia_actuary', '{"avgTat":21.5,"lossRatio":78.3,"frequency":1.2,"claimCount":9}'),
('REGION',     'MIDWEST',        'TAT,LOSS_RATIO,FREQUENCY',          '2026-05-01', 'nadia_actuary', '{"avgTat":18.1,"lossRatio":71.5,"frequency":1.3,"claimCount":10}'),
('REGION',     'WEST',           'TAT,LOSS_RATIO,FREQUENCY',          '2026-05-01', 'nadia_actuary', '{"avgTat":19.3,"lossRatio":73.7,"frequency":1.5,"claimCount":10}'),
('CLAIM_TYPE', 'AUTO',           'TAT,SEVERITY',                       '2026-04-30', 'sarah_analyst', '{"avgTat":15.3,"severity":12450}'),
('CLAIM_TYPE', 'PROPERTY',       'TAT,SEVERITY',                       '2026-04-30', 'sarah_analyst', '{"avgTat":21.4,"severity":76300}'),
('CLAIM_TYPE', 'HEALTH',         'TAT,SEVERITY',                       '2026-04-30', 'sarah_analyst', '{"avgTat":19.5,"severity":30425}'),
('CLAIM_TYPE', 'LIABILITY',      'TAT,SEVERITY',                       '2026-04-30', 'sarah_analyst', '{"avgTat":22.8,"severity":89300}'),
('CLAIM_TYPE', 'WC',             'TAT,SEVERITY',                       '2026-04-30', 'sarah_analyst', '{"avgTat":18.7,"severity":31985}'),
('PERIOD',     'Q1-2026',        'TAT,SEVERITY,LOSS_RATIO,FREQUENCY', '2026-04-01', 'lena_ops',      '{"avgTat":19.2,"severity":42100,"lossRatio":74.6,"frequency":1.3,"claimCount":15}'),
('PERIOD',     'Q4-2025',        'TAT,SEVERITY,LOSS_RATIO,FREQUENCY', '2026-01-02', 'lena_ops',      '{"avgTat":18.4,"severity":38200,"lossRatio":71.8,"frequency":1.2,"claimCount":13}'),
('PERIOD',     'Q3-2025',        'TAT,SEVERITY,LOSS_RATIO,FREQUENCY', '2025-10-01', 'lena_ops',      '{"avgTat":17.9,"severity":35800,"lossRatio":68.5,"frequency":1.1,"claimCount":11}'),
('PERIOD',     'Q2-2026',        'TAT,SEVERITY,LOSS_RATIO,FREQUENCY', '2026-05-01', 'lena_ops',      '{"avgTat":16.8,"severity":28400,"lossRatio":67.2,"frequency":1.0,"claimCount":4}'),
('PRODUCT',    'PROPERTY',       'FRAUD_RATE,LEAKAGE',                '2026-05-02', 'priya_fraud',   '{"fraudRate":33.3,"leakageEstimate":18500}'),
('PRODUCT',    'HEALTH',         'FRAUD_RATE,LEAKAGE',                '2026-05-02', 'priya_fraud',   '{"fraudRate":37.5,"leakageEstimate":5150}'),
('REGION',     'SOUTHEAST',      'FRAUD_RATE',                         '2026-05-02', 'priya_fraud',   '{"fraudRate":44.4,"providerWatchCount":1,"watchedProvider":"PRV-MIAMI-041"}'),
('PERIOD',     'Q1-2026',        'CYCLE_TIME,SLA_COMPLIANCE',         '2026-04-01', 'david_manager', '{"avgCycleTime":26.2,"slaComplianceRate":81.3}'),
('PERIOD',     'Q4-2025',        'CYCLE_TIME,SLA_COMPLIANCE',         '2026-01-02', 'david_manager', '{"avgCycleTime":24.8,"slaComplianceRate":85.5}'),
('PERIOD',     'Q1-2026',        'RESERVES,AGING',                    '2026-04-01', 'nadia_actuary', '{"totalReserves":1985200,"agingBucket":{"0-30":7,"31-60":5,"61-90":3,"90+":13}}');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- DONE — verify with these quick sanity queries:
--
--   USE claiminsight_identity_db; SELECT COUNT(*) FROM gateway_users;        -- 6
--   USE data_ingestion_db;        SELECT COUNT(*) FROM claim_raw;            -- 40
--   USE claims_metrics_db;        SELECT COUNT(*) FROM claim_kpi;            -- 240
--   USE fraud_risk_db;            SELECT COUNT(*) FROM risk_score;           -- 28
--   USE fraud_risk_db;            SELECT COUNT(*) FROM investigations;       -- 6
--   USE denial_leakage;           SELECT COUNT(*) FROM denial_pattern;       -- 14
--   USE claims_cost_reserve_db;   SELECT COUNT(*) FROM claim_cost;           -- ~110
--   USE AdjusterPerformanceDB;    SELECT COUNT(*) FROM adjuster_performance; -- 32
--   USE NotificationsDB;          SELECT COUNT(*) FROM notifications;        -- ~60
--   USE claims_analytics_report_db; SELECT COUNT(*) FROM analytics_report;   -- 24
--
-- Test login on the frontend:
--   admin_alice / Password1!     (Admin)
--   sarah_analyst / Password1!   (Claims Analyst)
--   david_manager / Password1!   (Claims Manager)
--   priya_fraud / Password1!     (Fraud Analyst — has the active SIU cases)
--   nadia_actuary / Password1!   (Actuary)
--   lena_ops / Password1!        (Operations Executive)
-- ============================================================================
