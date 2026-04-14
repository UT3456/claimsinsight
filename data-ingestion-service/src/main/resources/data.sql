-- ============================================================
-- ClaimInsight360 - Data Ingestion Service
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
-- SEED: data_feed (10 rows)
-- All FeedType values: CLAIM, POLICY, PAYMENT, RESERVE
-- All FeedStatus values: ACTIVE, INACTIVE, FAILED
-- ============================================================
INSERT IGNORE INTO data_feed (feed_id, feed_type, source_system, last_sync_date, status, created_date) VALUES
(1,  'CLAIM',   'ClaimsPro v3',          '2026-03-17 09:30:00', 'ACTIVE',   '2025-01-10 08:00:00'),
(2,  'CLAIM',   'FastClaim System',       '2026-03-16 14:00:00', 'ACTIVE',   '2025-02-01 09:00:00'),
(3,  'POLICY',  'PolicySys v2',           '2026-03-15 11:30:00', 'ACTIVE',   '2025-01-10 08:05:00'),
(4,  'POLICY',  'PolicyCore Enterprise',  '2026-03-14 10:00:00', 'ACTIVE',   '2025-03-01 10:00:00'),
(5,  'PAYMENT', 'PayTrack v1',            '2026-03-10 16:00:00', 'ACTIVE',   '2025-01-10 08:10:00'),
(6,  'PAYMENT', 'PayGateway Pro',         NULL,                  'INACTIVE', '2025-04-15 11:00:00'),
(7,  'RESERVE', 'ReserveMgr v4',          '2026-03-12 08:45:00', 'ACTIVE',   '2025-01-10 08:15:00'),
(8,  'RESERVE', 'ActuarialCalc v2',       '2026-02-28 17:00:00', 'ACTIVE',   '2025-05-01 12:00:00'),
(9,  'CLAIM',   'LegacyClaims v1',        NULL,                  'FAILED',   '2024-07-01 09:00:00'),
(10, 'POLICY',  'OldPolicySys v0.9',      NULL,                  'INACTIVE', '2024-06-01 08:00:00');

-- ============================================================
-- SEED: claim_raw (50 rows)
-- Feed 1  ClaimsPro v3         15 auto/property/medical claims
-- Feed 2  FastClaim System     10 auto/liability claims
-- Feed 3  PolicySys v2         10 policy records
-- Feed 4  PolicyCore           5  enterprise policy records
-- Feed 5  PayTrack v1          5  payment records
-- Feed 7  ReserveMgr v4        5  reserve records
-- Feeds 6,8,9,10 intentionally empty (INACTIVE/FAILED)
-- ============================================================

-- Feed 1: ClaimsPro v3
INSERT IGNORE INTO claim_raw (raw_id, claim_id, feed_id, payload_json, ingested_date) VALUES
(1,  'CLM-2026-AUTO-001', 1, '{"claimNumber":"CLM-2026-AUTO-001","policyNumber":"POL-AUTO-001","claimantName":"Alice Johnson","incidentDate":"2026-01-05","claimType":"Auto","vehicleMake":"Toyota","vehicleModel":"Camry","damageDescription":"Rear-end collision, bumper and trunk damage","claimAmount":8500.00,"repairEstimate":8200.00,"status":"Open","adjuster":"Mike Torres","priority":"Medium"}', '2026-01-06 09:15:00'),
(2,  'CLM-2026-AUTO-002', 1, '{"claimNumber":"CLM-2026-AUTO-002","policyNumber":"POL-AUTO-002","claimantName":"Bob Smith","incidentDate":"2026-01-08","claimType":"Auto","vehicleMake":"Honda","vehicleModel":"Civic","damageDescription":"Side swipe on passenger door and fender","claimAmount":4200.00,"repairEstimate":4000.00,"status":"Under Review","adjuster":"Sarah Lee","priority":"Low"}', '2026-01-09 10:30:00'),
(3,  'CLM-2026-AUTO-003', 1, '{"claimNumber":"CLM-2026-AUTO-003","policyNumber":"POL-AUTO-003","claimantName":"Carol White","incidentDate":"2026-01-12","claimType":"Auto","vehicleMake":"Ford","vehicleModel":"F-150","damageDescription":"Total loss front impact at 55mph","claimAmount":35000.00,"repairEstimate":38000.00,"status":"Total Loss","adjuster":"James Park","priority":"High"}', '2026-01-13 08:00:00'),
(4,  'CLM-2026-AUTO-004', 1, '{"claimNumber":"CLM-2026-AUTO-004","policyNumber":"POL-AUTO-004","claimantName":"David Brown","incidentDate":"2026-01-15","claimType":"Auto","vehicleMake":"BMW","vehicleModel":"3 Series","damageDescription":"Hail damage to roof and bonnet","claimAmount":12000.00,"repairEstimate":11500.00,"status":"Approved","adjuster":"Lisa Chen","priority":"Medium"}', '2026-01-16 11:20:00'),
(5,  'CLM-2026-AUTO-005', 1, '{"claimNumber":"CLM-2026-AUTO-005","policyNumber":"POL-AUTO-005","claimantName":"Eva Green","incidentDate":"2026-01-18","claimType":"Auto","vehicleMake":"Mercedes","vehicleModel":"C-Class","damageDescription":"Windscreen replacement stone chip","claimAmount":1800.00,"repairEstimate":1750.00,"status":"Closed","adjuster":"Tom Wilson","priority":"Low"}', '2026-01-19 14:00:00'),
(6,  'CLM-2026-PROP-001', 1, '{"claimNumber":"CLM-2026-PROP-001","policyNumber":"POL-HOME-001","claimantName":"Frank Miller","incidentDate":"2026-01-20","claimType":"Property","propertyAddress":"12 Oak Street London","damageType":"Water Damage","damageDescription":"Burst pipe caused flooding in kitchen and living room","claimAmount":22000.00,"status":"Open","adjuster":"Anna Scott","priority":"High"}', '2026-01-21 09:00:00'),
(7,  'CLM-2026-PROP-002', 1, '{"claimNumber":"CLM-2026-PROP-002","policyNumber":"POL-HOME-002","claimantName":"Grace Lee","incidentDate":"2026-01-22","claimType":"Property","propertyAddress":"7 Maple Avenue Manchester","damageType":"Fire","damageDescription":"Kitchen fire smoke damage throughout ground floor","claimAmount":45000.00,"status":"Under Investigation","adjuster":"Mike Torres","priority":"Critical"}', '2026-01-23 08:30:00'),
(8,  'CLM-2026-PROP-003', 1, '{"claimNumber":"CLM-2026-PROP-003","policyNumber":"POL-HOME-003","claimantName":"Henry Davis","incidentDate":"2026-01-25","claimType":"Property","propertyAddress":"33 Pine Road Birmingham","damageType":"Theft","damageDescription":"Burglary laptop TV and jewellery stolen","claimAmount":9500.00,"status":"Approved","adjuster":"Sarah Lee","priority":"Medium"}', '2026-01-26 10:00:00'),
(9,  'CLM-2026-MED-001',  1, '{"claimNumber":"CLM-2026-MED-001","policyNumber":"POL-MED-001","claimantName":"Isla Turner","incidentDate":"2026-02-01","claimType":"Medical","patientName":"Isla Turner","treatment":"Knee surgery ACL reconstruction","hospitalName":"St Thomas Hospital","admissionDate":"2026-02-01","dischargeDate":"2026-02-03","claimAmount":18500.00,"status":"Open","adjuster":"Lisa Chen","priority":"High"}', '2026-02-02 09:00:00'),
(10, 'CLM-2026-MED-002',  1, '{"claimNumber":"CLM-2026-MED-002","policyNumber":"POL-MED-002","claimantName":"Jack Robinson","incidentDate":"2026-02-05","claimType":"Medical","patientName":"Jack Robinson","treatment":"Emergency appendectomy","hospitalName":"Royal Free Hospital","admissionDate":"2026-02-05","dischargeDate":"2026-02-08","claimAmount":14200.00,"status":"Approved","adjuster":"James Park","priority":"High"}', '2026-02-06 07:30:00'),
(11, 'CLM-2026-MED-003',  1, '{"claimNumber":"CLM-2026-MED-003","policyNumber":"POL-MED-003","claimantName":"Karen Hughes","incidentDate":"2026-02-10","claimType":"Medical","patientName":"Karen Hughes","treatment":"Hip replacement surgery","hospitalName":"Kings College Hospital","admissionDate":"2026-02-10","dischargeDate":"2026-02-16","claimAmount":32000.00,"status":"Under Review","adjuster":"Anna Scott","priority":"Medium"}', '2026-02-11 10:15:00'),
(12, 'CLM-2026-AUTO-006', 1, '{"claimNumber":"CLM-2026-AUTO-006","policyNumber":"POL-AUTO-006","claimantName":"Liam Parker","incidentDate":"2026-02-14","claimType":"Auto","vehicleMake":"Audi","vehicleModel":"A4","damageDescription":"Pothole damage cracked alloy and puncture","claimAmount":650.00,"repairEstimate":620.00,"status":"Closed","adjuster":"Tom Wilson","priority":"Low"}', '2026-02-15 13:00:00'),
(13, 'CLM-2026-AUTO-007', 1, '{"claimNumber":"CLM-2026-AUTO-007","policyNumber":"POL-AUTO-007","claimantName":"Mia Collins","incidentDate":"2026-02-18","claimType":"Auto","vehicleMake":"Tesla","vehicleModel":"Model 3","damageDescription":"Vandalism keyed on both sides","claimAmount":3800.00,"repairEstimate":3750.00,"status":"Approved","adjuster":"Sarah Lee","priority":"Low"}', '2026-02-19 09:45:00'),
(14, 'CLM-2026-PROP-004', 1, '{"claimNumber":"CLM-2026-PROP-004","policyNumber":"POL-HOME-004","claimantName":"Noah Wright","incidentDate":"2026-03-01","claimType":"Property","propertyAddress":"88 Cedar Lane Leeds","damageType":"Storm","damageDescription":"Storm damaged roof tiles and guttering","claimAmount":7800.00,"status":"Open","adjuster":"Lisa Chen","priority":"Medium"}', '2026-03-02 08:00:00'),
(15, 'CLM-2026-MED-004',  1, '{"claimNumber":"CLM-2026-MED-004","policyNumber":"POL-MED-004","claimantName":"Olivia King","incidentDate":"2026-03-05","claimType":"Medical","patientName":"Olivia King","treatment":"Physiotherapy 12 sessions post-fracture","hospitalName":"Bupa Clinic","claimAmount":2400.00,"status":"Approved","adjuster":"James Park","priority":"Low"}', '2026-03-06 11:00:00');

-- Feed 2: FastClaim System
INSERT IGNORE INTO claim_raw (raw_id, claim_id, feed_id, payload_json, ingested_date) VALUES
(16, 'CLM-2026-FC-001', 2, '{"claimNumber":"CLM-2026-FC-001","claimantName":"Peter Adams","incidentDate":"2026-01-07","claimType":"Auto","vehicleMake":"Volkswagen","vehicleModel":"Golf","damageDescription":"Parking lot collision dent on driver door","claimAmount":2100.00,"status":"Closed","priority":"Low"}', '2026-01-08 08:30:00'),
(17, 'CLM-2026-FC-002', 2, '{"claimNumber":"CLM-2026-FC-002","claimantName":"Rachel Morgan","incidentDate":"2026-01-10","claimType":"Auto","vehicleMake":"Nissan","vehicleModel":"Qashqai","damageDescription":"Whiplash from rear shunt on motorway","claimAmount":15000.00,"injuryCompensation":13800.00,"status":"Open","priority":"High"}', '2026-01-11 09:00:00'),
(18, 'CLM-2026-FC-003', 2, '{"claimNumber":"CLM-2026-FC-003","claimantName":"Sam Taylor","incidentDate":"2026-01-16","claimType":"Liability","description":"Customer slipped on wet floor in insured premises","claimantInjury":"Fractured wrist","medicalCosts":5200.00,"legalCosts":3000.00,"totalClaim":8200.00,"status":"Under Review","priority":"High"}', '2026-01-17 14:15:00'),
(19, 'CLM-2026-FC-004', 2, '{"claimNumber":"CLM-2026-FC-004","claimantName":"Tina Brooks","incidentDate":"2026-01-20","claimType":"Auto","vehicleMake":"Range Rover","vehicleModel":"Evoque","damageDescription":"Flood damage engine hydrolocked","claimAmount":28000.00,"status":"Total Loss","priority":"Critical"}', '2026-01-21 10:00:00'),
(20, 'CLM-2026-FC-005', 2, '{"claimNumber":"CLM-2026-FC-005","claimantName":"Uma Patel","incidentDate":"2026-01-28","claimType":"Auto","vehicleMake":"Kia","vehicleModel":"Sportage","damageDescription":"Cracked windscreen pebble on A1","claimAmount":450.00,"status":"Closed","priority":"Low"}', '2026-01-29 09:30:00'),
(21, 'CLM-2026-FC-006', 2, '{"claimNumber":"CLM-2026-FC-006","claimantName":"Victor Chan","incidentDate":"2026-02-03","claimType":"Liability","description":"Product liability defective appliance caused property fire","damageAmount":62000.00,"legalCosts":15000.00,"totalClaim":77000.00,"status":"Under Investigation","priority":"Critical"}', '2026-02-04 08:00:00'),
(22, 'CLM-2026-FC-007', 2, '{"claimNumber":"CLM-2026-FC-007","claimantName":"Wendy Foster","incidentDate":"2026-02-07","claimType":"Auto","vehicleMake":"Mazda","vehicleModel":"CX-5","damageDescription":"Side mirror smashed hit and run","claimAmount":380.00,"status":"Approved","priority":"Low"}', '2026-02-08 11:00:00'),
(23, 'CLM-2026-FC-008', 2, '{"claimNumber":"CLM-2026-FC-008","claimantName":"Xander Hill","incidentDate":"2026-02-12","claimType":"Auto","vehicleMake":"Porsche","vehicleModel":"Cayenne","damageDescription":"Rear shunt at lights boot and exhaust damage","claimAmount":18500.00,"status":"Under Review","priority":"High"}', '2026-02-13 10:00:00'),
(24, 'CLM-2026-FC-009', 2, '{"claimNumber":"CLM-2026-FC-009","claimantName":"Yvonne Ross","incidentDate":"2026-02-20","claimType":"Liability","description":"Employer liability worker fell from scaffold","medicalCosts":22000.00,"lossOfEarnings":18000.00,"legalCosts":12000.00,"totalClaim":52000.00,"status":"Open","priority":"Critical"}', '2026-02-21 08:30:00'),
(25, 'CLM-2026-FC-010', 2, '{"claimNumber":"CLM-2026-FC-010","claimantName":"Zara Ahmed","incidentDate":"2026-03-02","claimType":"Auto","vehicleMake":"Hyundai","vehicleModel":"Tucson","damageDescription":"Catalytic converter theft overnight","claimAmount":2800.00,"status":"Approved","priority":"Medium"}', '2026-03-03 09:15:00');

-- Feed 3: PolicySys v2
INSERT IGNORE INTO claim_raw (raw_id, claim_id, feed_id, payload_json, ingested_date) VALUES
(26, 'POL-2025-001', 3, '{"policyNumber":"POL-2025-001","holderName":"Alice Johnson","policyType":"Comprehensive Auto","premium":1450.00,"excess":500.00,"startDate":"2025-01-01","endDate":"2026-01-01","status":"Active","coverageLimit":50000.00,"insuredVehicle":"Toyota Camry 2022"}', '2026-03-01 09:00:00'),
(27, 'POL-2025-002', 3, '{"policyNumber":"POL-2025-002","holderName":"Bob Smith","policyType":"Third Party Auto","premium":620.00,"excess":250.00,"startDate":"2025-03-01","endDate":"2026-03-01","status":"Active","coverageLimit":20000.00,"insuredVehicle":"Honda Civic 2020"}', '2026-03-01 09:05:00'),
(28, 'POL-2025-003', 3, '{"policyNumber":"POL-2025-003","holderName":"Carol White","policyType":"Home Buildings","premium":890.00,"excess":300.00,"startDate":"2025-06-01","endDate":"2026-06-01","status":"Active","coverageLimit":300000.00,"propertyAddress":"33 Pine Road Birmingham"}', '2026-03-01 09:10:00'),
(29, 'POL-2025-004', 3, '{"policyNumber":"POL-2025-004","holderName":"David Brown","policyType":"Home Contents","premium":320.00,"excess":150.00,"startDate":"2025-06-01","endDate":"2026-06-01","status":"Active","coverageLimit":50000.00}', '2026-03-01 09:15:00'),
(30, 'POL-2025-005', 3, '{"policyNumber":"POL-2025-005","holderName":"Eva Green","policyType":"Private Medical","premium":2400.00,"excess":100.00,"startDate":"2025-01-01","endDate":"2026-01-01","status":"Lapsed","coverageLimit":200000.00}', '2026-03-01 09:20:00'),
(31, 'POL-2025-006', 3, '{"policyNumber":"POL-2025-006","holderName":"Frank Miller","policyType":"Comprehensive Auto","premium":1800.00,"excess":500.00,"startDate":"2025-04-01","endDate":"2026-04-01","status":"Active","coverageLimit":75000.00,"insuredVehicle":"BMW 3 Series 2023"}', '2026-03-01 09:25:00'),
(32, 'POL-2025-007', 3, '{"policyNumber":"POL-2025-007","holderName":"Grace Lee","policyType":"Business Liability","premium":3500.00,"excess":1000.00,"startDate":"2025-01-01","endDate":"2026-01-01","status":"Active","coverageLimit":1000000.00,"businessName":"Lee Coffee Shops Ltd"}', '2026-03-01 09:30:00'),
(33, 'POL-2025-008', 3, '{"policyNumber":"POL-2025-008","holderName":"Henry Davis","policyType":"Travel","premium":180.00,"excess":75.00,"startDate":"2026-01-15","endDate":"2026-01-30","status":"Expired","coverageLimit":10000.00,"destination":"Thailand"}', '2026-03-01 09:35:00'),
(34, 'POL-2025-009', 3, '{"policyNumber":"POL-2025-009","holderName":"Isla Turner","policyType":"Private Medical","premium":3200.00,"excess":200.00,"startDate":"2025-07-01","endDate":"2026-07-01","status":"Active","coverageLimit":500000.00}', '2026-03-01 09:40:00'),
(35, 'POL-2025-010', 3, '{"policyNumber":"POL-2025-010","holderName":"Jack Robinson","policyType":"Landlord","premium":1100.00,"excess":500.00,"startDate":"2025-09-01","endDate":"2026-09-01","status":"Active","coverageLimit":250000.00,"propertyAddress":"5 Birch Close Bristol","rentalIncome":1500.00}', '2026-03-01 09:45:00');

-- Feed 4: PolicyCore Enterprise
INSERT IGNORE INTO claim_raw (raw_id, claim_id, feed_id, payload_json, ingested_date) VALUES
(36, 'POL-ENT-001', 4, '{"policyNumber":"POL-ENT-001","holderName":"Kingsley Logistics Ltd","policyType":"Fleet Auto","premium":28000.00,"excess":1000.00,"startDate":"2025-01-01","endDate":"2026-01-01","status":"Active","coverageLimit":5000000.00,"fleetSize":45}', '2026-03-14 10:05:00'),
(37, 'POL-ENT-002', 4, '{"policyNumber":"POL-ENT-002","holderName":"MedTech Solutions Ltd","policyType":"Professional Indemnity","premium":15000.00,"excess":5000.00,"startDate":"2025-04-01","endDate":"2026-04-01","status":"Active","coverageLimit":10000000.00}', '2026-03-14 10:10:00'),
(38, 'POL-ENT-003', 4, '{"policyNumber":"POL-ENT-003","holderName":"BuildRight Construction","policyType":"Employers Liability","premium":12000.00,"excess":2000.00,"startDate":"2025-06-01","endDate":"2026-06-01","status":"Active","coverageLimit":10000000.00,"employeeCount":120}', '2026-03-14 10:15:00'),
(39, 'POL-ENT-004', 4, '{"policyNumber":"POL-ENT-004","holderName":"FreshMarket Retail","policyType":"Commercial Property","premium":8500.00,"excess":2500.00,"startDate":"2025-01-01","endDate":"2026-01-01","status":"Active","coverageLimit":2500000.00,"locations":3}', '2026-03-14 10:20:00'),
(40, 'POL-ENT-005', 4, '{"policyNumber":"POL-ENT-005","holderName":"ClearWave Telecom","policyType":"Cyber Liability","premium":22000.00,"excess":10000.00,"startDate":"2025-09-01","endDate":"2026-09-01","status":"Active","coverageLimit":5000000.00,"annualRevenue":45000000.00}', '2026-03-14 10:25:00');

-- Feed 5: PayTrack v1
INSERT IGNORE INTO claim_raw (raw_id, claim_id, feed_id, payload_json, ingested_date) VALUES
(41, 'PAY-2026-001', 5, '{"paymentReference":"PAY-2026-001","claimNumber":"CLM-2026-AUTO-001","paymentType":"Settlement","payeeName":"Alice Johnson","amount":8500.00,"currency":"GBP","paymentDate":"2026-02-10","paymentMethod":"BACS","status":"Completed","authorisedBy":"Head of Claims"}', '2026-02-10 15:00:00'),
(42, 'PAY-2026-002', 5, '{"paymentReference":"PAY-2026-002","claimNumber":"CLM-2026-PROP-003","paymentType":"Settlement","payeeName":"Henry Davis","amount":9500.00,"currency":"GBP","paymentDate":"2026-02-12","paymentMethod":"BACS","status":"Completed","authorisedBy":"Claims Manager"}', '2026-02-12 16:30:00'),
(43, 'PAY-2026-003', 5, '{"paymentReference":"PAY-2026-003","claimNumber":"CLM-2026-MED-002","paymentType":"Medical","payeeName":"Royal Free Hospital","amount":14200.00,"currency":"GBP","paymentDate":"2026-02-15","paymentMethod":"CHAPS","status":"Completed","authorisedBy":"Medical Claims Team"}', '2026-02-15 14:00:00'),
(44, 'PAY-2026-004', 5, '{"paymentReference":"PAY-2026-004","claimNumber":"CLM-2026-FC-001","paymentType":"Repair","payeeName":"AutoFix Garage Ltd","amount":2100.00,"currency":"GBP","paymentDate":"2026-02-20","paymentMethod":"BACS","status":"Completed","authorisedBy":"Claims Assessor"}', '2026-02-20 11:00:00'),
(45, 'PAY-2026-005', 5, '{"paymentReference":"PAY-2026-005","claimNumber":"CLM-2026-AUTO-004","paymentType":"Settlement","payeeName":"David Brown","amount":12000.00,"currency":"GBP","paymentDate":"2026-03-01","paymentMethod":"BACS","status":"Pending","authorisedBy":"Claims Manager","notes":"Awaiting final repair invoice"}', '2026-03-01 10:30:00');

-- Feed 7: ReserveMgr v4
INSERT IGNORE INTO claim_raw (raw_id, claim_id, feed_id, payload_json, ingested_date) VALUES
(46, 'RES-2026-001', 7, '{"reserveId":"RES-2026-001","claimNumber":"CLM-2026-AUTO-003","reserveType":"Case Reserve","initialReserve":40000.00,"currentReserve":35000.00,"currency":"GBP","setDate":"2026-01-14","reservedBy":"James Park","status":"Active","notes":"Total loss awaiting salvage valuation"}', '2026-01-14 09:00:00'),
(47, 'RES-2026-002', 7, '{"reserveId":"RES-2026-002","claimNumber":"CLM-2026-PROP-002","reserveType":"Case Reserve","initialReserve":55000.00,"currentReserve":48000.00,"currency":"GBP","setDate":"2026-01-24","reservedBy":"Mike Torres","status":"Active","notes":"Fire damage contractor quotes received"}', '2026-01-24 10:00:00'),
(48, 'RES-2026-003', 7, '{"reserveId":"RES-2026-003","claimNumber":"CLM-2026-FC-006","reserveType":"Catastrophe Reserve","initialReserve":100000.00,"currentReserve":100000.00,"currency":"GBP","setDate":"2026-02-05","reservedBy":"Head Actuary","status":"Active","notes":"Product liability potential class action"}', '2026-02-05 08:30:00'),
(49, 'RES-2026-004', 7, '{"reserveId":"RES-2026-004","claimNumber":"CLM-2026-FC-009","reserveType":"IBNR","initialReserve":80000.00,"currentReserve":75000.00,"currency":"GBP","setDate":"2026-02-22","reservedBy":"Head Actuary","status":"Active","notes":"Employer liability ongoing legal proceedings"}', '2026-02-22 09:00:00'),
(50, 'RES-2026-005', 7, '{"reserveId":"RES-2026-005","claimNumber":"CLM-2026-MED-003","reserveType":"Case Reserve","initialReserve":35000.00,"currentReserve":32000.00,"currency":"GBP","setDate":"2026-02-12","reservedBy":"Lisa Chen","status":"Active","notes":"Hip replacement post-operative physiotherapy ongoing"}', '2026-02-12 11:00:00');
