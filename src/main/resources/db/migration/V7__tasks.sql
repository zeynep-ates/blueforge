CREATE TABLE task (
    id                  BIGSERIAL PRIMARY KEY,
    user_story_id       BIGINT NOT NULL REFERENCES user_story(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,
    priority            VARCHAR(20) NOT NULL CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    effort_estimate     VARCHAR(20) NOT NULL CHECK (effort_estimate IN ('S', 'M', 'L')),
    order_index         INT NOT NULL
);

CREATE INDEX idx_task_user_story_id ON task(user_story_id);
