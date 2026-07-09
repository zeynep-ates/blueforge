# Sprint 6 Summary

Product-focus pivot: with the AI planning pipeline complete end to end
(Sprint 5), this sprint shifted from extending the backend to making
BlueForge usable and demoable — a full React frontend for the existing
pipeline, plus the visual identity to go with it.

## What was built

- **Frontend scaffold**: Vite + React 19 + TypeScript, Tailwind v4 +
  shadcn/ui (Base UI primitives), React Router, TanStack Query. The API
  client and its query/mutation hooks are generated directly from the
  backend's OpenAPI spec via `orval` — no hand-written DTOs to drift out of
  sync with the Java side.
- **Two-panel workspace**: a sidebar showing pipeline progress (Questions →
  Requirements → Epics → User Stories → Tasks) next to a main content area
  where each stage is its own section, its "Generate X" action living
  inside that section. The UI's locked/current/done state is derived
  directly from `ProjectVersionResponse.status` — no separate frontend
  wizard state to keep in sync.
- **Hierarchical Epic → User Story → Task view**: replaced flat
  badge-annotated lists with a `shadcn/ui` accordion — User Stories grouped
  by Epic, Tasks nested two levels deep (Epic → User Story → task list),
  expanded by default with counts on every trigger.
- **Visual identity**: a blue-centered color system driven entirely by CSS
  custom properties (cascades through every component with no per-component
  edits needed), a custom logo mark (two angle brackets rotated into a
  faceted diamond — no container, no AI-cliché imagery), and full
  light/dark/system theme support (`ThemeProvider` + toggle +
  `localStorage` persistence + OS-preference default).
- **Tests**: Vitest + React Testing Library, 11 tests covering the pipeline
  stage-derivation logic and the shared `StageSection` component.
- **Bug fix (shipped separately from the frontend work)**: found via manual
  end-to-end verification, not a test — `project_version.status` was
  `VARCHAR(20)` since `V1__initial_schema.sql`, but
  `REQUIREMENTS_GENERATED` and `USER_STORIES_GENERATED` are 23 characters.
  Every real request that reached those statuses against Postgres failed
  with `value too long for type character varying(20)`. Fixed via
  `V9__widen_project_version_status.sql` (→ `VARCHAR(32)`), kept as its own
  commit and changelog entry rather than folded into the frontend work.
- **Minor backend touch**: a `WebConfig` CORS bean for the frontend's dev
  origin, gated behind a configurable `blueforge.cors.allowed-origins`
  property.

Every stage of the pipeline (idea → questions → answers → requirements →
epics → user stories → tasks) was verified manually in-browser against the
real backend and a live AI model, not just via component tests.

## Known limitations

- **No backend browse/list API yet.** The frontend can only reach a
  project version through its own creation flow, or a purely client-side
  `localStorage` "recent projects" list. There is still no
  `GET /api/projects` or `GET /api/projects/{id}/versions` — that's Sprint
  7, by design (see the agreed roadmap below).
- **No editing or regeneration of generated content.** If the AI gets an
  Epic or Task wrong, there's no way to fix it short of accepting it —
  intentionally deferred, not an oversight.
- **No authentication.** Every project is fully public; also intentional,
  placed after Browse+Edit in the agreed roadmap rather than before.
- **Single JS bundle, no code-splitting** (~504 KB / ~159 KB gzipped).
  Fine at current scope; worth revisiting with route-level lazy loading if
  the app grows.
- **OpenRouter free-tier model churn.** The default model configured in
  `application.yml` was found dead mid-sprint (a repeat of the same class
  of issue noted in the Sprint 1 summary) — this needs periodic attention
  independent of BlueForge's own code, not a BlueForge bug.

## Technical debt / carry-forward items

- Once Sprint 7 adds real list endpoints, replace the client-side-only
  "recent projects" `localStorage` list with a real backend-driven project
  browser.
- Consider code-splitting the frontend bundle (React Router lazy routes)
  if bundle size becomes a real concern.
- `application.yml`'s default `OPENROUTER_MODEL` should be revisited for
  long-term stability, or the README should document that free-tier model
  slugs are expected to need periodic swapping.

## Agreed roadmap (Sprint 7 onward)

Decided during this sprint's planning discussion, prioritizing product
value over further backend depth:

- **Sprint 7** — Browse + Edit APIs: list projects/versions, `PATCH`
  endpoints for Requirement/Epic/UserStory/Task content, plus the
  corresponding frontend browse/edit UI.
- **Sprint 8** — Authentication & authorization, project ownership.
- **Sprint 9** — Export (Markdown/PDF first, then Jira/GitHub
  integrations).
- **Sprint 10** — Regeneration of individual entities (with the
  cascade/status-model design work that requires).
