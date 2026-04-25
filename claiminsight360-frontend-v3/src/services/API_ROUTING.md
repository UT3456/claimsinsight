/**
 * API GATEWAY ROUTING CONFIGURATION
 * 
 * API Gateway runs on: http://localhost:8086
 * 
 * ROUTE MAPPING:
 * ─────────────────────────────────────────────────────────────────────
 * 
 * DATA INGESTION SERVICE (8082)
 *   /api/feeds/**        → lb://data-ingestion-service
 *   /api/ingest/**       → lb://data-ingestion-service
 * 
 * CLAIMS METRICS SERVICE (8083)
 *   /api/kpis/**         → lb://claims-metrics-service
 * 
 * COST RESERVE SERVICE (8089)
 *   /api/costs/**        → lb://cost-reserve-service
 *   /api/reserves/**     → lb://cost-reserve-service
 *   /api/aging/**        → lb://cost-reserve-service
 * 
 * ADJUSTER & OPERATIONS SERVICE (8087)
 *   /api/adjusters/**    → lb://AdjusterAndOperations
 *   /api/sla-violations/**  → lb://AdjusterAndOperations
 * 
 * NOTIFICATION SERVICE (8088)
 *   /api/notifications/** → lb://NotificationService
 * 
 * FRAUD RISK SERVICE
 *   /api/risk-scores/**  → lb://fraud-risk-service
 *   /api/risk-indicators/** → lb://fraud-risk-service
 * 
 * DENIAL & LEAKAGE SERVICE
 *   /api/denial-patterns/** → lb://denial-leakage-service
 *   /api/leakage-flags/** → lb://denial-leakage-service
 * 
 * ANALYTICS REPORT SERVICE
 *   /api/reports/**      → lb://analytics-report-service
 * 
 * IDENTITY/AUTH (Embedded in API Gateway on 8086)
 *   /auth/**             → Embedded in API Gateway
 * 
 * ─────────────────────────────────────────────────────────────────────
 * 
 * FRONTEND SERVICE ENDPOINTS:
 * 
 * Authentication:
 *   POST /auth/login         - Login
 *   POST /auth/logout        - Logout
 *   GET  /auth/me            - Current user
 *   POST /auth/refresh       - Refresh token
 * 
 * Claims (Data Ingestion):
 *   GET    /api/feeds/claims              - List claims
 *   GET    /api/feeds/claims/{id}         - Get claim
 *   POST   /api/feeds/claims              - Create claim
 *   PUT    /api/feeds/claims/{id}         - Update claim
 *   DELETE /api/feeds/claims/{id}         - Delete claim
 * 
 * Analytics/KPIs:
 *   GET /api/kpis/dashboard              - Dashboard metrics
 *   GET /api/kpis/summary                - Summary stats
 * 
 * Cost Reserves:
 *   GET    /api/reserves                 - List reserves
 *   GET    /api/reserves/{id}            - Get reserve
 *   POST   /api/reserves                 - Create reserve
 *   PUT    /api/reserves/{id}            - Update reserve
 *   DELETE /api/reserves/{id}            - Delete reserve
 * 
 * Adjusters:
 *   GET    /api/adjusters                - List adjusters
 *   GET    /api/adjusters/{id}           - Get adjuster
 *   POST   /api/adjusters                - Create adjuster
 *   PUT    /api/adjusters/{id}           - Update adjuster
 *   DELETE /api/adjusters/{id}           - Delete adjuster
 * 
 * Notifications:
 *   GET    /api/notifications            - List notifications
 *   POST   /api/notifications/read       - Mark as read
 * 
 * Fraud Risk:
 *   GET    /api/risk-scores              - List fraud risks
 *   GET    /api/risk-scores/{id}         - Get fraud risk
 *   POST   /api/risk-scores              - Create fraud risk
 * 
 * Denial & Leakage:
 *   GET    /api/denial-patterns          - Denial patterns
 *   GET    /api/leakage-flags            - Leakage flags
 * 
 * Reports:
 *   GET    /api/reports                  - List reports
 *   POST   /api/reports                  - Generate report
 *   GET    /api/reports/{id}             - Get report
 * 
 * ─────────────────────────────────────────────────────────────────────
 */
