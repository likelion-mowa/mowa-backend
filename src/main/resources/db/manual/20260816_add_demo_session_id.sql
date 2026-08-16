-- Manual PostgreSQL schema patch for demo-session isolation.
--
-- This project does not currently include Flyway, Liquibase, schema.sql, or
-- Docker init SQL. Apply this script manually to an empty MOWA database before
-- running the demo-session-isolated backend.
--
-- If any target table already has rows and is missing demo_session_id, this
-- script aborts instead of inventing or backfilling session ids.

BEGIN;

DO $$
DECLARE
    row_count BIGINT;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'walk_candidates'
          AND column_name = 'demo_session_id'
    ) THEN
        SELECT COUNT(*) INTO row_count FROM walk_candidates;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'walk_candidates has % existing rows; choose an explicit demo_session_id backfill policy before adding NOT NULL',
                row_count;
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'experience_drafts'
          AND column_name = 'demo_session_id'
    ) THEN
        SELECT COUNT(*) INTO row_count FROM experience_drafts;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'experience_drafts has % existing rows; choose an explicit demo_session_id backfill policy before adding NOT NULL',
                row_count;
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'walk_experiences'
          AND column_name = 'demo_session_id'
    ) THEN
        SELECT COUNT(*) INTO row_count FROM walk_experiences;
        IF row_count > 0 THEN
            RAISE EXCEPTION
                'walk_experiences has % existing rows; choose an explicit demo_session_id backfill policy before adding NOT NULL',
                row_count;
        END IF;
    END IF;
END $$;

ALTER TABLE walk_candidates
    ADD COLUMN IF NOT EXISTS demo_session_id UUID;

ALTER TABLE experience_drafts
    ADD COLUMN IF NOT EXISTS demo_session_id UUID;

ALTER TABLE walk_experiences
    ADD COLUMN IF NOT EXISTS demo_session_id UUID;

ALTER TABLE walk_candidates
    ALTER COLUMN demo_session_id SET NOT NULL;

ALTER TABLE experience_drafts
    ALTER COLUMN demo_session_id SET NOT NULL;

ALTER TABLE walk_experiences
    ALTER COLUMN demo_session_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_walk_experiences_user_demo_session_started_at
    ON walk_experiences (user_id, demo_session_id, started_at);

COMMIT;
