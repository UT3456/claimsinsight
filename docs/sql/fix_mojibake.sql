-- ====================================================================
-- Fix mojibake in denial_leakage.denial_pattern.
--
-- Symptom: dashboard shows  "Policy lapsed at time of incident ÔÇö no
-- coverage in force"  instead of  "... - no coverage in force".
--
-- Root cause: a previous seed file stored em-dashes (—, U+2014, UTF-8
-- bytes 0xE2 0x80 0x94). When that seed was loaded with the MySQL
-- client charset set to a DOS code page (cp437/cp850), the three UTF-8
-- bytes were each decoded as a separate character — 0xE2→Ô, 0x80→Ç,
-- 0x94→ö — and stored as the literal string "ÔÇö" in a utf8mb4 column.
-- The current seed_all_databases.sql is already fixed; this script
-- patches existing databases without re-seeding.
--
-- Usage:
--   mysql --default-character-set=utf8mb4 -u root -p denial_leakage < fix_mojibake.sql
--
-- Safe to run multiple times (no-op once cleaned).
-- ====================================================================

SET NAMES utf8mb4;

USE denial_leakage;

-- Em-dash (—) misread as ÔÇö → plain hyphen to match the rest of the seed
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇö', '-');

-- En-dash (–) misread as ÔÇô → plain hyphen
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇô', '-');

-- Horizontal ellipsis (…) misread as ÔÇª → three dots
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇª', '...');

-- Curly single quotes (' ') misread as ÔÇÿ / ÔÇÖ → straight apostrophe
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇÿ', '''');
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇÖ', '''');

-- Curly double quotes (" ") misread as ÔÇ£ / ÔÇ¥ → straight double quote
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇ£', '"');
UPDATE denial_pattern SET reason = REPLACE(reason, 'ÔÇ¥', '"');

-- Alternative mojibake form (Windows-1252 misread of em-dash) — defensive
UPDATE denial_pattern SET reason = REPLACE(reason, 'â€"',  '-');
UPDATE denial_pattern SET reason = REPLACE(reason, 'â€¦',  '...');

-- Verify
SELECT pattern_id, reason FROM denial_pattern WHERE reason REGEXP 'Ô|â€' LIMIT 20;
