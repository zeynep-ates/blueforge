CREATE TABLE architecture_recommendation (
    id                  BIGSERIAL PRIMARY KEY,
    project_version_id  BIGINT NOT NULL REFERENCES project_version(id),
    component           VARCHAR(255) NOT NULL,
    recommendation      VARCHAR(255) NOT NULL,
    reasoning           TEXT NOT NULL,
    tradeoffs           TEXT NOT NULL,
    order_index         INT NOT NULL
);

CREATE INDEX idx_architecture_recommendation_project_version_id ON architecture_recommendation(project_version_id);
