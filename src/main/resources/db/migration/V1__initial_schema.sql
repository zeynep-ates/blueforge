CREATE TABLE project (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE project_version (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES project(id),
    version_number      INT NOT NULL,
    idea_snapshot       TEXT NOT NULL,
    change_description  TEXT,
    status              VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'AWAITING_ANSWERS')),
    UNIQUE (project_id, version_number)
);

CREATE TABLE clarifying_question (
    id                  BIGSERIAL PRIMARY KEY,
    project_version_id  BIGINT NOT NULL REFERENCES project_version(id),
    question_text       TEXT NOT NULL,
    order_index         INT NOT NULL
);

CREATE INDEX idx_project_version_project_id ON project_version(project_id);
CREATE INDEX idx_clarifying_question_project_version_id ON clarifying_question(project_version_id);
