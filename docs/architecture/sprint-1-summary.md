# Sprint 1 Summary

Foundation sprint: project skeleton through a working, AI-backed create/read
flow for a project's first version. Five checkpoints, summarized here for
the record; see [`decisions.md`](decisions.md) for the reasoning behind
individual choices and [`domain-model.md`](domain-model.md) for the current
schema.

## What was built

- **Skeleton**: Spring Boot 3.5 / Java 21, Postgres via `docker-compose`.
- **Domain model + migration**: `Project`, `ProjectVersion`,
  `ClarifyingQuestion` entities backed by `V1__initial_schema.sql`, with
  Hibernate `ddl-auto=validate` keeping entities and schema honest.
- **`AiClient`**: a one-method (`complete(String prompt)`) provider-agnostic
  interface with an `OpenRouterAiClient` implementation on Spring's
  `RestClient`. Wire format and HTTP failures stay private to the
  implementation.
- **`POST /api/projects`**: creates a `Project` + `ProjectVersion` v1, calls
  the AI with a classpath-loaded prompt template, parses a strict JSON
  question list, and persists everything in one transaction. AI/parse
  failures return a clean `502`, no retry, no orphaned rows on failure.
- **`GET /api/projects/{projectId}/versions/{versionNumber}`**: returns a
  stored version with its ordered clarifying questions via an explicit
  `JOIN FETCH` query; unknown project/version returns `404`.
- **Test coverage**: 11 tests across `AiClient`, `ProjectService`, and
  `ProjectController` (mocked HTTP, mocked repositories, `MockMvc` slice
  tests) — no test depends on a live AI key.

Every checkpoint was also verified manually against a real Postgres
container (Flyway migration applied, Hibernate validation passed, live curl
against running endpoints), not just unit tests.

## Known limitation: live OpenRouter smoke test is pending

Sprint 1's plan included one true end-to-end smoke test — `POST` then `GET`
against real OpenRouter, no mocks. That test is currently **blocked**, not
failing:

- The default free-tier model configured in `application.yml`
  (`meta-llama/llama-3.1-8b-instruct:free`) has been discontinued by
  OpenRouter as a free slug (`404`).
- Several other current free-tier slugs (`meta-llama/llama-3.3-70b-instruct:free`,
  `meta-llama/llama-3.2-3b-instruct:free`, `nousresearch/hermes-3-llama-3.1-405b:free`)
  return `429` from a shared upstream provider ("Venice") that's congested
  across all of OpenRouter's free users, independent of our API key or code.
- A valid API key was confirmed working (a raw call against it returns a
  proper `user_id`, not an auth error) — this is a provider-capacity issue,
  not a configuration or implementation bug.

No implementation changes were made to work around this (no retry loop, no
silent model-switching) — that would contradict the project's explicit
no-retry policy and mask a real availability question the config should
answer deliberately, not accidentally. Follow-up: pick and pin a currently-stable
free model before Sprint 2 closes, or switch to a low-cost paid model for
local development and demo recording (the project plan already anticipates
this trade-off in its cost note). Until then, `POST /api/projects` is
verified correct against every failure mode we could exercise (invalid key →
`401`/`502`, malformed AI JSON → `502`, blank input → `400`, successful
persistence path via mocked `AiClient` in tests) — the one thing not yet
observed is a real model's JSON coming back successfully end-to-end.

## Technical debt / carry-forward items

- **Pin a working free-tier model.** Current default is dead; needs a
  replacement verified against live OpenRouter, or an explicit decision to
  develop against a paid model locally.
- **No integration test with a real database.** All persistence-path
  verification so far has been manual (curl + psql) per checkpoint. A
  Testcontainers-backed test that boots the app against a real Postgres and
  exercises `POST` → `GET` (with a mocked `AiClient`) would give this
  regression coverage without needing network access or an API key in CI.
- **No API docs yet.** springdoc-openapi is in the project plan's "Must
  have" list but hasn't been added — worth doing before the surface area
  grows much further.
- **`ProjectService` is one class doing prompt-building, AI-response
  parsing, persistence, and DTO mapping.** Still cohesive for a single
  use case (see the Checkpoint 5 assessment), but Sprint 2 adds a second,
  larger AI-driven step (`POST .../answers`) — that's the natural point to
  extract shared prompt-loading/JSON-parsing helpers if the duplication
  actually shows up, not before.
- **No structured error envelope beyond `{ "message": "..." }`.** Fine at
  today's surface area; would be worth a consistent error code/type field
  if more endpoints and failure modes accumulate.

## Proposed Sprint 2 goals

Per the project plan's Week 2 scope, pending your confirmation of checkpoint
boundaries:
- `POST /api/projects/{id}/versions/{v}/answers`: full synchronous pipeline
  (functional requirements → non-functional requirements → epics/user
  stories/tasks → architecture recommendation with reasoning and trade-offs).
- Persist the full `Epic → UserStory → Task` hierarchy (new entities +
  migration).
- Clean error responses on AI/parse failure at each pipeline stage, still
  no retry.
- Swagger/OpenAPI docs (springdoc-openapi).
