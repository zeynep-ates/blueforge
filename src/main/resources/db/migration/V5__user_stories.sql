CREATE TABLE user_story (
    id                   BIGSERIAL PRIMARY KEY,
    epic_id              BIGINT NOT NULL REFERENCES epic(id),
    title                VARCHAR(255) NOT NULL,
    description          TEXT NOT NULL,
    acceptance_criteria  TEXT NOT NULL,
    order_index          INT NOT NULL
);

CREATE INDEX idx_user_story_epic_id ON user_story(epic_id);
