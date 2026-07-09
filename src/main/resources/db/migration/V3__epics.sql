CREATE TABLE epic (
    id                  BIGSERIAL PRIMARY KEY,
    project_version_id  BIGINT NOT NULL REFERENCES project_version(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,
    order_index         INT NOT NULL
);

CREATE INDEX idx_epic_project_version_id ON epic(project_version_id);
