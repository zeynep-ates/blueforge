# Architecture Decisions

Short log of decisions and the reasoning behind them, newest first. Not full
ADRs — a paragraph each, enough for future-us to know *why* without re-deriving it.

## Hibernate `ddl-auto=validate`, Flyway owns the schema
Migrations are the single source of truth. Hibernate only checks entity
mappings against what Flyway created at startup and fails fast on drift,
instead of `update` silently generating DDL that never gets reviewed.

## Lazy fetch on all `@ManyToOne` associations
`ProjectVersion.project` and `ClarifyingQuestion.projectVersion` are
`FetchType.LAZY` so loading a child never implicitly pulls its parent graph.

## Cascade + orphan removal on `ProjectVersion → ClarifyingQuestion`
Questions have no lifecycle independent of their version — removing one from
the list should delete it, so the relation is
`cascade = ALL, orphanRemoval = true`.

## `BIGSERIAL` / `IDENTITY` primary keys, not UUIDs
Nothing in current scope needs client-generated or non-sequential IDs;
sequential bigints are simpler and keep indexes smaller.

## Protected no-args constructors on entities
JPA requires a no-arg constructor for proxying, but entities shouldn't be
constructible in an invalid state from application code. Lombok
`@NoArgsConstructor(access = PROTECTED)` satisfies JPA while forcing real
code through the explicit, field-validating constructors.

## Synchronous pipeline, no async/job entity
A single-user portfolio demo doesn't have a concurrency or latency problem
worth solving with `@Async`, job tracking, or polling — that would be
complexity simulating a scale problem that doesn't exist yet. See
[`blueforge-project-plan.md` §9](../../blueforge-project-plan.md#9-future-improvements-explicitly-out-of-scope-for-v1)
for the evolution path if that changes.
