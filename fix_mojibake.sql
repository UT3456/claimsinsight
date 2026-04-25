-- Fix mojibake in denial_leakage and any other tables that stored em-dashes
-- with the wrong charset. Run this against each service DB that shows garbled
-- text. Safe to run multiple times.
--
-- Usage (example):
--   mysql --default-character-set=utf8mb4 -u root -p denial_leakage_db < fix_mojibake.sql

SET NAMES utf8mb4;

-- Denial-leakage reasons (main culprit on the dashboard)
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, CHAR(0xC3, 0xA2, 0xE2, 0x82, 0xAC, 0xE2, 0x80, 0x9D USING utf8mb4), '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'ÔçÒ', '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'ÒçÜ', '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'ÔçÖ', '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'ÔçÓ', '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'â€"',  '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'â€"',  '-');
UPDATE denial_reasons SET reason_text = REPLACE(reason_text, 'â€¦',  '...');

-- Generic column sweeps for other tables that might hold descriptive text
-- Uncomment and adapt per service if you see mojibake elsewhere.
-- UPDATE fraud_signals    SET description = REPLACE(description, 'ÔçÒ', '-');
-- UPDATE notifications    SET message     = REPLACE(message,     'ÔçÒ', '-');
-- UPDATE adjuster_metrics SET notes       = REPLACE(notes,       'ÔçÒ', '-');
