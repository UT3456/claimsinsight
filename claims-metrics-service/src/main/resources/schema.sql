-- ============================================================
-- ClaimInsight360 - Claims Metrics Service
-- schema.sql -> DDL for optional SQL-based initialization
--
-- Spring Boot also looks for data.sql in the same folder:
--   schema.sql runs first  - creates the tables
--   data.sql   runs second - inserts the seed rows
--
-- BOTH FILES run automatically only when application.yml has:
--   spring.jpa.hibernate.ddl-auto=none
--   spring.sql.init.mode=always
--
-- With ddl-auto=update (current default) BOTH files are skipped.
-- Hibernate creates the tables itself from @Entity classes.
-- ============================================================

-- ============================================================
-- Table: claim_kpi
-- One row = one KPI metric for one claim on one date.
-- ============================================================
CREATE TABLE IF NOT EXISTS claim_kpi (
    kpi_id       BIGINT         NOT NULL AUTO_INCREMENT,
    claim_id     VARCHAR(100)   NOT NULL,
    metric_name  VARCHAR(50)    NOT NULL,
    metric_value DECIMAL(15, 4) NOT NULL,
    metric_date  DATE           NOT NULL,

    CONSTRAINT pk_claim_kpi PRIMARY KEY (kpi_id)
);

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_claim_kpi_claim_id    ON claim_kpi (claim_id);
CREATE INDEX IF NOT EXISTS idx_claim_kpi_metric_name ON claim_kpi (metric_name);
CREATE INDEX IF NOT EXISTS idx_claim_kpi_metric_date ON claim_kpi (metric_date);
