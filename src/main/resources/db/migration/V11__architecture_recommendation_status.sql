-- V8 named this constraint explicitly, so it can be dropped by name directly.
ALTER TABLE project_version DROP CONSTRAINT project_version_status_check;

ALTER TABLE project_version ADD CONSTRAINT project_version_status_check
    CHECK (status IN ('AWAITING_ANSWERS', 'REQUIREMENTS_GENERATED', 'EPICS_GENERATED', 'USER_STORIES_GENERATED', 'TASKS_GENERATED', 'ARCHITECTURE_GENERATED'));
