-- Replace the CHECK constraint on project_version.status to allow REQUIREMENTS_GENERATED.
-- The constraint name is looked up from the system catalog instead of assumed, since V1
-- (already released as v0.1.0) declared it inline without an explicit name.
DO $$
DECLARE
    existing_constraint_name text;
BEGIN
    SELECT con.conname INTO existing_constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'project_version'
      AND con.contype = 'c'
      AND att.attname = 'status';

    IF existing_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE project_version DROP CONSTRAINT %I', existing_constraint_name);
    END IF;
END $$;

ALTER TABLE project_version ADD CONSTRAINT project_version_status_check
    CHECK (status IN ('DRAFT', 'AWAITING_ANSWERS', 'REQUIREMENTS_GENERATED'));

CREATE TABLE clarifying_answer (
    id                      BIGSERIAL PRIMARY KEY,
    clarifying_question_id  BIGINT NOT NULL UNIQUE REFERENCES clarifying_question(id),
    answer_text             TEXT NOT NULL,
    answered_at             TIMESTAMP NOT NULL
);

CREATE TABLE requirement (
    id                  BIGSERIAL PRIMARY KEY,
    project_version_id  BIGINT NOT NULL REFERENCES project_version(id),
    type                VARCHAR(20) NOT NULL CHECK (type IN ('FUNCTIONAL', 'NON_FUNCTIONAL', 'CONSTRAINT', 'ASSUMPTION')),
    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,
    order_index         INT NOT NULL
);

CREATE INDEX idx_requirement_project_version_id ON requirement(project_version_id);
