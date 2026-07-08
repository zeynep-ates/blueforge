# Architecture Decisions

Short log of decisions and the reasoning behind them, newest first. Not full
ADRs — a paragraph each, enough for future-us to know *why* without re-deriving it.

## `AiClient` stays minimal for now — `complete(String prompt)` only
The interface intentionally exposes a single plain-text method rather than
anticipating structured outputs, streaming, multiple providers, or tool
calling. Every one of those adds real shape to an interface (response
schemas, chunked callbacks, provider capability flags, tool/function
definitions) and right now there's exactly one call site and one provider —
designing for flexibility nothing yet exercises would be guessing at
requirements the pipeline doesn't have. Expected triggers to revisit this:
- **Structured outputs**: once callers need reliable JSON back (the
  blueprint pipeline will), add a method that takes a schema/type and
  returns a parsed DTO rather than raw text, instead of pushing every
  caller to parse JSON out of `complete()`'s string.
- **Streaming**: only if a synchronous 10-90s response stops being
  acceptable (see the synchronous-pipeline decision below) — not before.
- **Multiple providers**: if a second provider is actually added, not in
  anticipation of one; the interface already isolates callers from
  OpenRouter, so this should mean a new implementation, not a redesign.
- **Tool calling**: only if a feature needs the model to invoke functions
  mid-generation, which nothing in the current scope does.

## `AiClient` interface hides the provider, `RestClient` over `WebClient`
`OpenRouterAiClient` is the only class allowed to know it's talking to
OpenRouter — the interface exposes just `String complete(String prompt)`,
and OpenRouter's wire format (chat-completions request/response shape) is
private to the implementation as nested records. Swapping providers later is
a new `AiClient` implementation, not a change to callers. Used Spring's
synchronous `RestClient` rather than `WebClient`: the whole pipeline is
synchronous by design (see below), so a reactive HTTP client would add a
dependency and a mental model this project doesn't use anywhere else.
Failures are wrapped in `AiClientException` so callers never see
`RestClientException` — consistent with "AiClient never leaks
provider-specific types."

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
