-- ============================================================================
-- ClaimInsight360 - Comprehensive Sample Data with Insurance Claims
-- ============================================================================

USE claiminsight_db;

-- ============================================================================
-- 1. INSERT GATEWAY USERS (for authentication/authorization)
-- ============================================================================
INSERT INTO gateway_users (username, email, password, name, phone, role, enabled, created_at) VALUES
('adjuster_john', 'john.adjuster@claiminsight.com', '$2a$10$slYQmyNdGzin7olVB4G.OPST9/PgBLbVWHmKTqM7SVCIT8T.5VR0m', 'John Anderson', '555-0101', 'ROLE_CLAIMS_ANALYST', b'1', NOW()),
('adjuster_sarah', 'sarah.review@claiminsight.com', '$2a$10$slYQmyNdGzin7olVB4G.OPST9/PgBLbVWHmKTqM7SVCIT8T.5VR0m', 'Sarah Johnson', '555-0102', 'ROLE_CLAIMS_MANAGER', b'1', NOW()),
('fraud_analyst', 'mike.fraud@claiminsight.com', '$2a$10$slYQmyNdGzin7olVB4G.OPST9/PgBLbVWHmKTqM7SVCIT8T.5VR0m', 'Mike Thompson', '555-0103', 'ROLE_FRAUD_ANALYST', b'1', NOW()),
('admin_user', 'admin@claiminsight.com', '$2a$10$slYQmyNdGzin7olVB4G.OPST9/PgBLbVWHmKTqM7SVCIT8T.5VR0m', 'System Admin', '555-0104', 'ROLE_ADMIN', b'1', NOW()),
('actuary_david', 'david.actuary@claiminsight.com', '$2a$10$slYQmyNdGzin7olVB4G.OPST9/PgBLbVWHmKTqM7SVCIT8T.5VR0m', 'David Martinez', '555-0105', 'ROLE_ACTUARY', b'1', NOW()),
('ops_exec', 'ops@claiminsight.com', '$2a$10$slYQmyNdGzin7olVB4G.OPST9/PgBLbVWHmKTqM7SVCIT8T.5VR0m', 'Operations Lead', '555-0106', 'ROLE_OPERATIONS_EXEC', b'1', NOW())
ON DUPLICATE KEY UPDATE email=VALUES(email);

-- ============================================================================
-- 2. INSERT DATA FEEDS (claim sources/channels)
-- ============================================================================
INSERT INTO data_feed (feed_type, source_system, status, created_date) VALUES
('CLAIM', 'auto_insurance_portal', 'ACTIVE', NOW()),
('CLAIM', 'health_claims_system', 'ACTIVE', NOW()),
('CLAIM', 'property_claims_feed', 'ACTIVE', NOW()),
('CLAIM', 'tpa_adjuster_system', 'ACTIVE', NOW()),
('CLAIM', 'manual_entry_system', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE source_system=VALUES(source_system);

-- ============================================================================
-- 3. INSERT CLAIM RAW DATA (with JSON payloads containing claim details)
-- ============================================================================
INSERT INTO claim_raw (claim_id, feed_id, payload_json, ingested_date) VALUES
('CLM-2024-001', 1, 
  JSON_OBJECT(
    'claimantName', 'Robert Williams',
    'claimantEmail', 'robert.williams@email.com',
    'claimantPhone', '(555) 123-4567',
    'incidentDate', '2024-02-15',
    'claimDate', '2024-02-20',
    'settlementDate', '2024-04-10',
    'claimAmount', 45000.00,
    'claimStatus', 'SETTLED',
    'claimType', 'AUTO_COLLISION',
    'vehicleInfo', 'Honda Civic 2020, Damage: Front-left collision',
    'policyNumber', 'POL-AUTO-2023-0001',
    'lossDescription', 'Vehicle hit by another car in intersection'
  ), NOW()),
  
('CLM-2024-002', 1,
  JSON_OBJECT(
    'claimantName', 'Emily Chen',
    'claimantEmail', 'emily.chen@email.com',
    'claimantPhone', '(555) 234-5678',
    'incidentDate', '2024-03-05',
    'claimDate', '2024-03-10',
    'settlementDate', NULL,
    'claimAmount', 78500.00,
    'claimStatus', 'PENDING_REVIEW',
    'claimType', 'AUTO_COMPREHENSIVE',
    'vehicleInfo', 'Toyota Camry 2021, Damage: Comprehensive - Hail damage',
    'policyNumber', 'POL-AUTO-2023-0002',
    'lossDescription', 'Severe hail storm damaged vehicle'
  ), NOW()),

('CLM-2024-003', 2,
  JSON_OBJECT(
    'claimantName', 'Michael Rodriguez',
    'claimantEmail', 'michael.r@email.com',
    'claimantPhone', '(555) 345-6789',
    'incidentDate', '2024-01-20',
    'claimDate', '2024-01-25',
    'settlementDate', '2024-03-15',
    'claimAmount', 125000.00,
    'claimStatus', 'SETTLED',
    'claimType', 'HEALTH_MEDICAL',
    'procedureDescription', 'Emergency surgery and hospitalization',
    'policyNumber', 'POL-HEALTH-2023-0003',
    'lossDescription', 'Emergency room visit and 5-day hospital stay'
  ), NOW()),

('CLM-2024-004', 3,
  JSON_OBJECT(
    'claimantName', 'Sandra Lopez',
    'claimantEmail', 'sandra.lopez@email.com',
    'claimantPhone', '(555) 456-7890',
    'incidentDate', '2024-03-12',
    'claimDate', '2024-03-15',
    'settlementDate', NULL,
    'claimAmount', 250000.00,
    'claimStatus', 'OPEN',
    'claimType', 'PROPERTY_DAMAGE',
    'propertyAddress', '123 Oak Street, Springfield, IL 62701',
    'policyNumber', 'POL-PROP-2023-0004',
    'lossDescription', 'House fire - total loss of primary residence'
  ), NOW()),

('CLM-2024-005', 1,
  JSON_OBJECT(
    'claimantName', 'James Patterson',
    'claimantEmail', 'james.p@email.com',
    'claimantPhone', '(555) 567-8901',
    'incidentDate', '2024-02-28',
    'claimDate', '2024-03-05',
    'settlementDate', NULL,
    'claimAmount', 35000.00,
    'claimStatus', 'DENIED',
    'claimType', 'AUTO_LIABILITY',
    'vehicleInfo', 'Nissan Altima 2019',
    'policyNumber', 'POL-AUTO-2023-0005',
    'lossDescription', 'At-fault accident - policy exclusion applied'
  ), NOW()),

('CLM-2024-006', 4,
  JSON_OBJECT(
    'claimantName', 'Lisa Anderson',
    'claimantEmail', 'lisa.a@email.com',
    'claimantPhone', '(555) 678-9012',
    'incidentDate', '2024-03-08',
    'claimDate', '2024-03-10',
    'settlementDate', '2024-04-05',
    'claimAmount', 62500.00,
    'claimStatus', 'SETTLED',
    'claimType', 'AUTO_MEDICAL_PAYMENTS',
    'vehicleInfo', 'Multiple vehicle accident',
    'policyNumber', 'POL-AUTO-2023-0006',
    'lossDescription', 'Medical expenses from traffic accident'
  ), NOW()),

('CLM-2024-007', 2,
  JSON_OBJECT(
    'claimantName', 'Christopher Lee',
    'claimantEmail', 'chris.lee@email.com',
    'claimantPhone', '(555) 789-0123',
    'incidentDate', '2024-03-01',
    'claimDate', '2024-03-05',
    'settlementDate', NULL,
    'claimAmount', 95000.00,
    'claimStatus', 'PENDING_REVIEW',
    'claimType', 'HEALTH_DENTAL',
    'procedureDescription', 'Orthodontic treatment - braces and aligners',
    'policyNumber', 'POL-HEALTH-2023-0007',
    'lossDescription', 'Comprehensive dental treatment plan'
  ), NOW()),

('CLM-2024-008', 3,
  JSON_OBJECT(
    'claimantName', 'Jennifer Watson',
    'claimantEmail', 'jen.watson@email.com',
    'claimantPhone', '(555) 890-1234',
    'incidentDate', '2024-02-20',
    'claimDate', '2024-02-25',
    'settlementDate', NULL,
    'claimAmount', 185000.00,
    'claimStatus', 'OPEN',
    'claimType', 'PROPERTY_WATER_DAMAGE',
    'propertyAddress', '456 Maple Avenue, Portland, OR 97214',
    'policyNumber', 'POL-PROP-2023-0008',
    'lossDescription', 'Pipe burst and extensive water damage'
  ), NOW()),

('CLM-2024-009', 5,
  JSON_OBJECT(
    'claimantName', 'Kevin Brown',
    'claimantEmail', 'kevin.b@email.com',
    'claimantPhone', '(555) 901-2345',
    'incidentDate', '2024-01-15',
    'claimDate', '2024-01-20',
    'settlementDate', '2024-02-28',
    'claimAmount', 55000.00,
    'claimStatus', 'SETTLED',
    'claimType', 'AUTO_UNINSURED_MOTORIST',
    'vehicleInfo', 'Ford F-150 2021',
    'policyNumber', 'POL-AUTO-2023-0009',
    'lossDescription', 'Hit by uninsured driver'
  ), NOW()),

('CLM-2024-010', 1,
  JSON_OBJECT(
    'claimantName', 'Michelle Taylor',
    'claimantEmail', 'michelle.t@email.com',
    'claimantPhone', '(555) 012-3456',
    'incidentDate', '2024-03-18',
    'claimDate', '2024-03-20',
    'settlementDate', NULL,
    'claimAmount', 42000.00,
    'claimStatus', 'APPROVED',
    'claimType', 'AUTO_RENTAL_REIMBURSEMENT',
    'vehicleInfo', 'Vehicle in repair - rental car approved',
    'policyNumber', 'POL-AUTO-2023-0010',
    'lossDescription', 'Rental car expenses while vehicle repaired'
  ), NOW())
ON DUPLICATE KEY UPDATE ingested_date=NOW();

-- ============================================================================
-- 4. INSERT CLAIM KPIs (calculated metrics)
-- ============================================================================
INSERT INTO claim_kpi (claim_id, metric_name, metric_value, metric_date) VALUES
('CLM-2024-001', 'TAT', 49, CURDATE()),
('CLM-2024-001', 'CYCLE_TIME', 49, CURDATE()),
('CLM-2024-001', 'SEVERITY', 4, CURDATE()),
('CLM-2024-001', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-001', 'LOSS_RATIO', 0.85, CURDATE()),
('CLM-2024-001', 'SETTLEMENT_TIME', 49, CURDATE()),
('CLM-2024-002', 'TAT', 26, CURDATE()),
('CLM-2024-002', 'CYCLE_TIME', 26, CURDATE()),
('CLM-2024-002', 'SEVERITY', 5, CURDATE()),
('CLM-2024-002', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-002', 'LOSS_RATIO', 0.95, CURDATE()),
('CLM-2024-003', 'TAT', 53, CURDATE()),
('CLM-2024-003', 'CYCLE_TIME', 53, CURDATE()),
('CLM-2024-003', 'SEVERITY', 5, CURDATE()),
('CLM-2024-003', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-003', 'LOSS_RATIO', 0.99, CURDATE()),
('CLM-2024-003', 'SETTLEMENT_TIME', 53, CURDATE()),
('CLM-2024-004', 'TAT', 3, CURDATE()),
('CLM-2024-004', 'CYCLE_TIME', 3, CURDATE()),
('CLM-2024-004', 'SEVERITY', 5, CURDATE()),
('CLM-2024-004', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-004', 'LOSS_RATIO', 1.00, CURDATE()),
('CLM-2024-005', 'TAT', 20, CURDATE()),
('CLM-2024-005', 'CYCLE_TIME', 20, CURDATE()),
('CLM-2024-005', 'SEVERITY', 3, CURDATE()),
('CLM-2024-005', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-005', 'LOSS_RATIO', 0.50, CURDATE()),
('CLM-2024-006', 'TAT', 26, CURDATE()),
('CLM-2024-006', 'CYCLE_TIME', 26, CURDATE()),
('CLM-2024-006', 'SEVERITY', 3, CURDATE()),
('CLM-2024-006', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-006', 'LOSS_RATIO', 0.75, CURDATE()),
('CLM-2024-006', 'SETTLEMENT_TIME', 26, CURDATE()),
('CLM-2024-007', 'TAT', 14, CURDATE()),
('CLM-2024-007', 'CYCLE_TIME', 14, CURDATE()),
('CLM-2024-007', 'SEVERITY', 4, CURDATE()),
('CLM-2024-007', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-007', 'LOSS_RATIO', 0.88, CURDATE()),
('CLM-2024-008', 'TAT', 6, CURDATE()),
('CLM-2024-008', 'CYCLE_TIME', 6, CURDATE()),
('CLM-2024-008', 'SEVERITY', 5, CURDATE()),
('CLM-2024-008', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-008', 'LOSS_RATIO', 0.92, CURDATE()),
('CLM-2024-009', 'TAT', 39, CURDATE()),
('CLM-2024-009', 'CYCLE_TIME', 39, CURDATE()),
('CLM-2024-009', 'SEVERITY', 4, CURDATE()),
('CLM-2024-009', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-009', 'LOSS_RATIO', 0.80, CURDATE()),
('CLM-2024-009', 'SETTLEMENT_TIME', 39, CURDATE()),
('CLM-2024-010', 'TAT', 1, CURDATE()),
('CLM-2024-010', 'CYCLE_TIME', 1, CURDATE()),
('CLM-2024-010', 'SEVERITY', 2, CURDATE()),
('CLM-2024-010', 'FREQUENCY', 1, CURDATE()),
('CLM-2024-010', 'LOSS_RATIO', 0.45, CURDATE())
ON DUPLICATE KEY UPDATE metric_date=CURDATE();

-- ============================================================================
-- 5. VERIFY DATA INSERTION
-- ============================================================================
SELECT '✅ Data Insertion Complete!' AS Status;
SELECT COUNT(*) as Total_Users FROM gateway_users;
SELECT COUNT(*) as Total_Data_Feeds FROM data_feed;
SELECT COUNT(*) as Total_Raw_Claims FROM claim_raw;
SELECT COUNT(*) as Total_KPI_Records FROM claim_kpi;

-- ============================================================================
-- 6. SAMPLE QUERIES TO VERIFY DATA
-- ============================================================================
SELECT '--- SAMPLE CLAIMS SUMMARY ---' AS Query_Title;
SELECT 
  cr.claim_id,
  JSON_EXTRACT(cr.payload_json, '$.claimantName') as Claimant,
  JSON_EXTRACT(cr.payload_json, '$.claimAmount') as Amount,
  JSON_EXTRACT(cr.payload_json, '$.claimStatus') as Status
FROM claim_raw cr
LIMIT 5;

SELECT '--- USER ROLES DISTRIBUTION ---' AS Query_Title;
SELECT role, COUNT(*) as Count FROM gateway_users GROUP BY role;
