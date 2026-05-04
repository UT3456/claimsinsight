-- ============================================================================
-- ClaimInsight360 - Sample Data Insertion
-- ============================================================================

-- Insert test users into api-gateway database
USE claiminsight_db;

-- Insert Users
INSERT INTO users (username, email, name, phone, password, role, status, created_at, updated_at) VALUES
('adjuster1', 'adjuster1@claiminsight.com', 'John Adjuster', '555-0001', '$2a$10$..encoded..', 'ADJUSTER', 'ACTIVE', NOW(), NOW()),
('adjuster2', 'adjuster2@claiminsight.com', 'Jane Reviewer', '555-0002', '$2a$10$..encoded..', 'REVIEWER', 'ACTIVE', NOW(), NOW()),
('admin', 'admin@claiminsight.com', 'Admin User', '555-0003', '$2a$10$..encoded..', 'ADMIN', 'ACTIVE', NOW(), NOW()),
('operator1', 'operator1@claiminsight.com', 'Mike Operator', '555-0004', '$2a$10$..encoded..', 'OPERATOR', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Insert Claims data
INSERT INTO claims (claim_id, claimant_name, claim_status, incident_date, claim_date, settlement_date, claim_amount, reserve_amount, created_at) VALUES
('CLM001', 'Alice Johnson', 'OPEN', DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), NULL, 50000.00, 35000.00, DATE_SUB(NOW(), INTERVAL 40 DAY)),
('CLM002', 'Bob Smith', 'APPROVED', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 75000.00, 65000.00, DATE_SUB(NOW(), INTERVAL 55 DAY)),
('CLM003', 'Carol White', 'PENDING_REVIEW', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL, 120000.00, 100000.00, DATE_SUB(NOW(), INTERVAL 25 DAY)),
('CLM004', 'David Brown', 'REJECTED', DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 85 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 45000.00, 0.00, DATE_SUB(NOW(), INTERVAL 85 DAY)),
('CLM005', 'Eve Davis', 'SETTLED', DATE_SUB(NOW(), INTERVAL 120 DAY), DATE_SUB(NOW(), INTERVAL 115 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), 95000.00, 90000.00, DATE_SUB(NOW(), INTERVAL 115 DAY))
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Insert Fraud Risk Data
USE fraud_risk_db;

INSERT INTO fraud_indicators (claim_id, fraud_score, risk_level, indicators, flagged_at) VALUES
('CLM001', 0.32, 'LOW', 'routine_claim', NOW()),
('CLM002', 0.15, 'LOW', 'clean_documentation', NOW()),
('CLM003', 0.68, 'HIGH', 'inconsistent_statements,inflated_amount', NOW()),
('CLM004', 0.85, 'CRITICAL', 'suspicious_patterns,multiple_inconsistencies', NOW()),
('CLM005', 0.25, 'LOW', 'standard_processing', NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Insert Claims Metrics
USE claims_analytics_report_db;

INSERT INTO claim_metrics (claim_id, metric_name, metric_value, calculated_date) VALUES
('CLM001', 'TAT', 40, NOW()),
('CLM001', 'CYCLE_TIME', 40, NOW()),
('CLM001', 'SEVERITY', 3, NOW()),
('CLM001', 'FREQUENCY', 1, NOW()),
('CLM002', 'TAT', 55, NOW()),
('CLM002', 'CYCLE_TIME', 50, NOW()),
('CLM002', 'SETTLEMENT_TIME', 50, NOW()),
('CLM003', 'TAT', 25, NOW()),
('CLM003', 'CYCLE_TIME', 25, NOW()),
('CLM003', 'SEVERITY', 5, NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Insert Cost Reserve Data
USE claims_cost_reserve_db;

INSERT INTO cost_reserves (claim_id, initial_reserve, current_reserve, recovery_amount, recovery_date, status) VALUES
('CLM001', 35000.00, 32000.00, 0.00, NULL, 'OPEN'),
('CLM002', 65000.00, 65000.00, 65000.00, DATE_SUB(NOW(), INTERVAL 5 DAY), 'SETTLED'),
('CLM003', 100000.00, 95000.00, 0.00, NULL, 'UNDER_REVIEW'),
('CLM004', 0.00, 0.00, 0.00, NULL, 'REJECTED'),
('CLM005', 90000.00, 90000.00, 88000.00, DATE_SUB(NOW(), INTERVAL 20 DAY), 'SETTLED')
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Insert Denial Leakage Data
USE denial_leakage;

INSERT INTO denial_cases (claim_id, denial_reason, denial_amount, denial_date, status) VALUES
('CLM004', 'INSUFFICIENT_EVIDENCE', 45000.00, DATE_SUB(NOW(), INTERVAL 85 DAY), 'DENIED'),
('CLM003', 'PENDING_INVESTIGATION', 25000.00, DATE_SUB(NOW(), INTERVAL 25 DAY), 'UNDER_REVIEW')
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Insert Notifications
USE notificationsdb;

INSERT INTO notifications (user_id, claim_id, notification_type, message, status, created_at) VALUES
(1, 'CLM001', 'CLAIM_OPENED', 'Claim CLM001 has been opened for processing', 'SENT', NOW()),
(2, 'CLM002', 'CLAIM_APPROVED', 'Claim CLM002 has been approved', 'SENT', NOW()),
(2, 'CLM003', 'CLAIM_FLAGGED', 'Claim CLM003 flagged for high fraud risk', 'SENT', NOW()),
(1, 'CLM004', 'CLAIM_REJECTED', 'Claim CLM004 has been rejected', 'SENT', NOW()),
(2, 'CLM005', 'CLAIM_SETTLED', 'Claim CLM005 has been settled', 'SENT', NOW())
ON DUPLICATE KEY UPDATE sent_at=NOW();

-- Insert Adjuster Performance Data
USE adjusterperformancedb;

INSERT INTO adjuster_performance (adjuster_id, claim_id, processing_time_days, quality_score, status) VALUES
(1, 'CLM001', 40, 8.5, 'IN_PROGRESS'),
(2, 'CLM002', 55, 9.2, 'COMPLETED'),
(2, 'CLM003', 25, 6.8, 'IN_PROGRESS'),
(1, 'CLM004', 85, 7.0, 'COMPLETED'),
(2, 'CLM005', 115, 8.8, 'COMPLETED')
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Summary
SELECT '✅ Sample data inserted successfully!' AS Status;
SELECT COUNT(*) as total_claims FROM claiminsight_db.claims;
SELECT COUNT(*) as total_users FROM claiminsight_db.users;
SELECT COUNT(*) as fraud_flags FROM fraud_risk_db.fraud_indicators;
SELECT COUNT(*) as notifications FROM notificationsdb.notifications;
