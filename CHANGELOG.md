# Changelog

All notable changes to BlueForge are documented in this file.

## [0.11.0] - 2026-07-10

### Added

- `POST /api/projects/{projectId}/versions/{versionNumber}/architecture-recommendations`
  — the pipeline's final stage. One AI call, fed the idea, requirements, and
  epics, returns a JSON array of `{component, recommendation, reasoning,
  tradeoffs}` entries (e.g. Backend Framework, Database, Deployment), each
  naming a credible alternative that was considered and rejected. Requires
  `TASKS_GENERATED`; transitions the version to the new terminal
  `ARCHITECTURE_GENERATED` status. New `ArchitectureRecommendation` entity
  (flat, top-level, like `Requirement`) via `V10__architecture_recommendations.sql`
  / `V11__architecture_recommendation_status.sql`.
- "Architecture Recommendations" section in the workspace, following the
  same generate/regenerate shape as every other stage. No edit endpoint for
  this stage — scoped out deliberately, can be added later the same way it
  was for the other four if it turns out to be needed.
- Regenerating to this stage clones everything through Tasks (a new
  `cloneTasksInto` helper — the only stage-clone step that didn't already
  exist) and re-runs just the recommendations.
- Added to version diffing (flat-list, like Requirements) and to the
  Markdown export (`## Architecture Recommendations` section).

## [0.10.0] - 2026-07-10

### Added

- `GET /api/projects/{projectId}/versions/{versionNumber}/export?format=markdown`
  — renders a version's full blueprint (idea, clarifying Q&A, requirements
  grouped by type, and the Epic → User Story → Task roadmap) as a Markdown
  document. Reads directly from the entity graph rather than the DTO layer,
  since `Epic.userStories` / `UserStory.tasks` are already properly nested —
  unlike the flat, FK-linked lists on `ProjectVersionResponse`. `format` is
  the only query param; any value other than `markdown` returns 400 via a
  new `UnsupportedExportFormatException`, so the endpoint has room to grow a
  `json` option later without an inconsistent shape. Sections for
  not-yet-reached pipeline stages are simply omitted. The response sets
  `Content-Disposition: attachment` with a slugified `{project-name}-v{n}.md`
  filename, so hitting the endpoint directly downloads a file.
- "Export" button in the workspace header — a plain anchor to the export
  endpoint (no fetch/blob handling, no generated API client involved), same
  "frontend has no business logic" precedent as the diff feature.

### Changed

- `docs/architecture/domain-model.md` refreshed — it still described the
  "Checkpoint 2" schema (`Project` / `ProjectVersion` / `ClarifyingQuestion`
  only) despite Epics, User Stories, Tasks, regeneration, and version diffing
  having shipped since.

## [0.9.0] - 2026-07-09

### Added

- `POST /api/projects/{projectId}/versions/{versionNumber}/regenerate` —
  regenerates a previously-reached pipeline stage (Requirements, Epics, User
  Stories, or Tasks) from a base version. Instead of mutating the base
  version in place, it clones everything up to that stage into a brand-new
  `ProjectVersion` and re-runs AI generation for just that stage — the base
  version, and any manual edits in it, are left untouched. This is the first
  feature to create a version other than `1`, activating the `versionNumber`
  and `changeDescription` columns that existed since `V1__initial_schema.sql`
  but were previously unused.
- "Regenerate" action in each pipeline section (Requirements/Epics/User
  Stories/Tasks) once that stage is done, via a new `RegenerateDialog` with
  an optional note. Confirming navigates to the newly created version.
- A "Regenerated: ..." indicator in the workspace header for any version
  with `versionNumber > 1`.
- `GET /api/projects/{projectId}/versions/{fromVersion}/diff/{toVersion}` —
  compares two versions of the same project, returning which Requirements,
  Epics, User Stories, and Tasks were added, removed, or modified between
  them, plus a `summary` block (added/removed/modified/unchanged counts) so
  future timeline/history views don't need to walk the whole tree just to
  show basic stats.
- Matching is positional (by `orderIndex`, hierarchical: Requirements and
  Epics at the top level, User Stories matched within their Epic pair,
  Tasks matched within their User Story pair) via a new `VersionEntityMatcher`
  interface / `PositionalEntityMatcher` implementation — the same
  interface-plus-implementation shape already used for `AiClient` /
  `OpenRouterAiClient`. A future lineage-based matcher (tracking clone
  provenance) can replace it without changing `VersionDiffService`,
  `EntityDiffBuilder`, or the response DTOs.
- `changeDescription` added to `ProjectVersionSummaryResponse` so the
  version picker can show why a version exists (e.g. "Regenerated from v1:
  ...").
- "Compare versions" control on the project detail page (two `Select`
  dropdowns, defaulting to the two most recent versions) and a new
  `VersionDiffPage` rendering the comparison — Requirements as a flat list,
  Epics as a nested accordion (Epic → User Stories → Tasks), each entry
  color-coded by change type. The frontend only calls the diff endpoint and
  renders the response; no comparison logic lives in React.

Regeneration is only allowed for a stage the base version has already
reached (`InvalidRegenerationTargetException` / 400 for an invalid target,
`RegenerationNotAllowedException` / 409 if the stage hasn't been reached
yet). No new Flyway migration was needed for either feature — both reuse
the existing `ProjectVersionStatus` values, entity model, and schema as-is.

### Fixed

- Default `OPENROUTER_MODEL` (`meta-llama/llama-3.1-8b-instruct:free`) had
  been discontinued by OpenRouter as a free-tier slug, matching the same
  class of issue already noted in `docs/architecture/sprint-1-summary.md`.
  Switched the default to `google/gemma-4-26b-a4b-it:free`.
- The `/projects` list page's cached latest-version/status could go stale
  after a regeneration; the projects-list query is now invalidated
  alongside the project-detail query.

## [0.8.0] - 2026-07-09

### Added

- `PATCH /api/requirements/{id}` — edit a requirement's title and
  description.
- `PATCH /api/epics/{id}` — edit an epic's title and description.
- `PATCH /api/user-stories/{id}` — edit a user story's title, description,
  and acceptance criteria.
- `PATCH /api/tasks/{id}` — edit a task's title and description.
- Dialog-based editing UI for Requirements, Epics, User Stories, and Tasks,
  backed by a reusable `EditItemDialog` component.
- `MethodArgumentNotValidException` handler, so validation failures on the
  new edit endpoints return the same `ErrorResponse` shape as every other
  endpoint.

Each edit endpoint returns only the updated entity (not the full
`ProjectVersionResponse`); the frontend patches the single matching item in
the cached version instead of refetching.

## [0.7.0] - 2026-07-09

### Added

- `GET /api/projects` — lists every project with its latest version number
  and status.
- `GET /api/projects/{id}` — project detail, including every version.
- `GET /api/projects/{id}/versions` — a project's versions in order.
- `/projects` frontend page: browse all projects in a table (name, latest
  version, status, created date).
- `/projects/:projectId` frontend page: project detail with a table of
  versions, linking into the existing pipeline workspace.
- "Back to project" link in the workspace sidebar, for navigating from a
  version back to its parent project.

### Fixed

- Base UI's `Button` warned in the console whenever rendered polymorphically
  as a router `Link` (`nativeButton` mismatch). It now defaults
  `nativeButton` to `false` whenever a `render` prop is supplied.

## [0.6.0] - 2026-07-09

### Added

- React frontend (Vite + TypeScript + Tailwind v4 + shadcn/ui) covering the
  full pipeline: Idea → Clarifying Questions → Requirements → Epics → User
  Stories → Tasks.
- Typed API client generated from the backend's OpenAPI spec via `orval`,
  paired with TanStack Query hooks for every endpoint.
- Two-panel workspace UI: sidebar pipeline progress + main content sections,
  each stage's generation action living inside its own section.
- Hierarchical Epic → User Story → Task visualization using an accordion,
  replacing flat badge-annotated lists.
- Blue-centered visual identity: CSS-variable-driven color system, a custom
  logo mark, and full light/dark/system theme support with persistence.
- Vitest + React Testing Library test setup (pipeline state-derivation logic
  and shared section component).
- Minimal backend CORS configuration for the frontend's dev/deployed origin.
- `docs/architecture/sprint-6-summary.md`.

### Fixed

- `project_version.status` was declared `VARCHAR(20)` since the initial
  schema, but two status values (`REQUIREMENTS_GENERATED`,
  `USER_STORIES_GENERATED`) are 23 characters — every real request reaching
  those statuses failed against Postgres with
  `value too long for type character varying(20)`. Widened to `VARCHAR(32)`
  via `V9__widen_project_version_status.sql`.

## [0.5.0] - 2026-07-09

### Added

- Task generation from User Stories, one AI call per Epic, with priority
  and effort-estimate fields.

## [0.4.0] - 2026-07-09

### Added

- User Story generation from Epics, including acceptance criteria, in a
  single AI call for all epics in a version.

## [0.3.0] - 2026-07-09

### Added

- Epic generation from a version's Requirements.

## [0.2.0] - 2026-07-09

### Added

- `POST /api/projects/{id}/versions/{v}/answers` — persists clarifying
  question answers and synchronously generates functional/non-functional
  requirements.

## [0.1.0] - 2026-07-08

### Added

- Project skeleton: Spring Boot 3.5, Java 21, PostgreSQL, Flyway.
- Provider-agnostic `AiClient` with an OpenRouter implementation.
- `POST /api/projects` — creates a project and its first version, generates
  AI clarifying questions.
- `GET /api/projects/{id}/versions/{v}` — retrieves a stored version.
- Swagger/OpenAPI documentation.
- GitHub Actions CI build.
