# Domain Model

Current state as of v0.10.0. See [`blueforge-project-plan.md`](../../blueforge-project-plan.md#6-data-model)
for the original target schema — `ArchitectureRecommendation` is the one piece
of that plan not yet built.

```
Project
 ├─ id, name, createdAt
 └─ versions: List<ProjectVersion>

ProjectVersion
 ├─ id, project_id, versionNumber, ideaSnapshot, changeDescription (nullable, v2+)
 ├─ status (AWAITING_ANSWERS | REQUIREMENTS_GENERATED | EPICS_GENERATED |
 │          USER_STORIES_GENERATED | TASKS_GENERATED)
 ├─ clarifyingQuestions: List<ClarifyingQuestion>
 ├─ requirements: List<Requirement>
 └─ epics: List<Epic>

ClarifyingQuestion
 ├─ id, projectVersion_id, questionText, orderIndex
 └─ answer: ClarifyingAnswer (nullable until answered)

ClarifyingAnswer
 ├─ id, clarifyingQuestion_id, answerText, answeredAt

Requirement
 ├─ id, projectVersion_id, type (FUNCTIONAL | NON_FUNCTIONAL | CONSTRAINT | ASSUMPTION)
 ├─ title, description, orderIndex

Epic
 ├─ id, projectVersion_id, title, description, orderIndex
 └─ userStories: List<UserStory>

UserStory
 ├─ id, epic_id, title, description, acceptanceCriteria, orderIndex
 └─ tasks: List<Task>

Task
 ├─ id, userStory_id, title, description
 ├─ priority (HIGH | MEDIUM | LOW)
 ├─ effortEstimate (S | M | L)
 └─ orderIndex
```

- `Project 1—* ProjectVersion` — a project accumulates versions over time.
  Every version other than `1` is created either by regenerating a stage
  (`POST .../regenerate`, cloning everything up to that stage into a new
  version) or, eventually, by a full idea revision; nothing mutates an
  existing version's rows once created.
- Every parent-child relation (`ProjectVersion → ClarifyingQuestion`,
  `ProjectVersion → Requirement`, `ProjectVersion → Epic`, `Epic → UserStory`,
  `UserStory → Task`, `ClarifyingQuestion → ClarifyingAnswer`) is cascaded
  with orphan removal — children have no lifecycle independent of their
  parent.
- `orderIndex` on every child entity is what "flat DTO lists reconstructed
  into a hierarchy" (Markdown export, version diffing) and "positional
  matching across versions" (`PositionalEntityMatcher`) both rely on —
  it's the one field every generation step is required to set correctly.
- There is no `ChangeType` column on any entity: change tracking is computed
  on demand by `VersionDiffService` when comparing two versions (via
  `VersionEntityMatcher` / `PositionalEntityMatcher`), not stored per-row.
  This diverges from the original plan's per-item `changeType` field — see
  the `[0.9.0]` entry in `CHANGELOG.md` for the reasoning.
- `ArchitectureRecommendation` (recommendation / reasoning / trade-offs per
  version) from the original plan does not exist yet.

Schema source of truth: [`src/main/resources/db/migration/`](../../src/main/resources/db/migration/).
Entities: [`src/main/java/com/blueforge/entity/`](../../src/main/java/com/blueforge/entity/).
