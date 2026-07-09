# Changelog

All notable changes to BlueForge are documented in this file.

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
