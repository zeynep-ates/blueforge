-- V1 defined status as VARCHAR(20), but REQUIREMENTS_GENERATED and
-- USER_STORIES_GENERATED are 23 characters, causing inserts/updates to fail
-- against real Postgres. Widen with headroom for future status values.
ALTER TABLE project_version ALTER COLUMN status TYPE VARCHAR(32);
