# Domain Model

Current state as of Checkpoint 2. See [`blueforge-project-plan.md`](../../blueforge-project-plan.md#6-data-model)
for the full target schema (`Requirement`, `RoadmapEpic`, `UserStory`, `Task`,
`ArchitectureRecommendation` land in later checkpoints).

```
Project
 ├─ id, name, createdAt
 └─ versions: List<ProjectVersion>

ProjectVersion
 ├─ id, project_id, versionNumber, ideaSnapshot, changeDescription (nullable)
 ├─ status (DRAFT | AWAITING_ANSWERS)
 └─ clarifyingQuestions: List<ClarifyingQuestion>

ClarifyingQuestion
 ├─ id, projectVersion_id, questionText, orderIndex
```

- `Project 1—* ProjectVersion` — a project accumulates versions over time; each
  revision creates a new `ProjectVersion` rather than mutating the last one.
- `ProjectVersion 1—* ClarifyingQuestion`, cascaded — questions have no
  lifecycle independent of their version.
- `status` will grow `COMPLETED` / `FAILED` once the blueprint pipeline
  (Checkpoint 3+) exists.

Schema source of truth: [`V1__initial_schema.sql`](../../src/main/resources/db/migration/V1__initial_schema.sql).
Entities: [`src/main/java/com/blueforge/entity/`](../../src/main/java/com/blueforge/entity/).
