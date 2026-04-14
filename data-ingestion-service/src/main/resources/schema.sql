-- ============================================================
-- ClaimInsight360 - Data Ingestion Service
-- schema.sql  →  DDL: creates tables and indexes
--
-- Spring Boot also looks for data.sql in the same folder:
--   schema.sql  runs first  — creates the tables
--   data.sql    runs second — inserts the seed rows
--
-- BOTH FILES run automatically only when application.properties has:
--   spring.jpa.hibernate.ddl-auto=none
--   spring.sql.init.mode=always
--
-- With ddl-auto=update (current default) BOTH files are SKIPPED.
-- Hibernate creates the tables itself from @Entity classes.
-- ============================================================

-- ============================================================
-- Table: data_feed
-- ============================================================
CREATE TABLE IF NOT EXISTS data_feed (
    feed_id        BIGINT       NOT NULL AUTO_INCREMENT,
    feed_type      VARCHAR(50)  NOT NULL,
    source_system  VARCHAR(100) NOT NULL,
    last_sync_date DATETIME     NULL,
    status         VARCHAR(20)  NOT NULL,
    created_date   DATETIME     NOT NULL,

    CONSTRAINT pk_data_feed PRIMARY KEY (feed_id)
);

-- ============================================================
-- Table: claim_raw
-- ============================================================
CREATE TABLE IF NOT EXISTS claim_raw (
    raw_id        BIGINT       NOT NULL AUTO_INCREMENT,
    claim_id      VARCHAR(100) NOT NULL,
    feed_id       BIGINT       NOT NULL,
    payload_json  LONGTEXT     NOT NULL,
    ingested_date DATETIME     NOT NULL,

    CONSTRAINT pk_claim_raw      PRIMARY KEY (raw_id),
    CONSTRAINT fk_claim_raw_feed FOREIGN KEY (feed_id)
        REFERENCES data_feed (feed_id) ON DELETE CASCADE
);

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_claim_raw_claim_id ON claim_raw (claim_id);
CREATE INDEX IF NOT EXISTS idx_claim_raw_feed_id  ON claim_raw (feed_id);
CREATE INDEX IF NOT EXISTS idx_data_feed_status   ON data_feed (status);
