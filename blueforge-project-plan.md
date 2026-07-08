# BlueForge
### From Software Idea to Technical Blueprint

**Type:** Portfolio project (not a commercial product)
**Optimized for:** Interview signal — Java backend architecture, AI integration, clean code, business-analysis thinking
**Timeline:** 3-4 weeks, one developer, part-time
**Philosophy:** Every feature earns its place by making the project more impressive in a backend interview — not by looking production-scale.

---

## 1. What It Does

The user describes a software idea in plain language. BlueForge acts like a senior business analyst / solution architect: it asks a bounded round of clarifying questions, then generates functional requirements, non-functional requirements, user stories broken into epics and tasks, an architecture recommendation *with reasoning and trade-offs*, and persists all of it as structured, queryable data. Users can revise the idea later ("remove payment integration") and get a new version with every item tagged as added, removed, modified, or unchanged.

---

## 2. Workflow (Synchronous MVP)

```
Idea submitted
   ↓
AI generates clarification questions        ← one AI call, returned to client
   ↓
User answers
   ↓
AI generates the complete blueprint          ← several AI calls, chained,
   ↓                                            all within one request
Everything is saved
   ↓
Final structured response returned
```

Both AI-heavy steps are synchronous HTTP calls. A 10-20 second response time for the blueprint-generation step is acceptable — this is a single-user portfolio demo, not a system serving concurrent traffic. No background jobs, no polling, no job-status entity. **Why this is the right call for an MVP:** async processing, job tracking, and polling only pay off when you have latency or scale problems worth solving. At one request at a time, they'd be infrastructure built to simulate a production concern that doesn't exist yet — the exact kind of complexity interviewers see through immediately. Section 9 covers how this evolves later, so the decision reads as intentional rather than a gap.

Similarly: **no automatic retry logic** for malformed AI JSON in this version. If a call fails or returns something unparseable, the endpoint returns a clean error (e.g. `502` with a message); the user just resubmits. For a synchronous, single-user tool, "the client retries by asking again" is a completely reasonable failure mode — building retry/backoff logic is worth doing once there's an actual reliability requirement, not before.

---

## 3. Scope — Must / Should / Could / Won't

**Must have:**
- Structured relational data model (Epic → UserStory → Task hierarchy)
- Provider-agnostic `AiClient` → `OpenRouterAiClient`
- Bounded workflow: idea → questions → answers → full blueprint (synchronous)
- Architecture recommendations with reasoning + trade-offs, not just a list
- Versioning with AI-computed change tags
- Markdown export
- Swagger/OpenAPI docs
- Core unit tests (services, mocked `AiClient`)
- Docker Compose (app + Postgres)
- Minimal frontend to demo the flow

**Should have (if weeks 1-2 go smoothly):**
- Jira-style JSON export
- A couple of integration tests beyond the happy path

**Could have (cut first under time pressure):**
- Simple API-key auth
- PDF export
- Multi-project comparison view

**Won't have in this version (see Section 9 for the evolution path):**
- `@Async` processing / `GenerationJob` entity / polling endpoints
- Retry logic for malformed AI responses
- Kafka / RabbitMQ / Elasticsearch / Redis
- Multi-tenant / enterprise features

---

## 4. Technology Stack

| Layer | Choice | Why |
|---|---|---|
| Language/Framework | Java 21, Spring Boot 3.x | Matches your CV directly |
| Database | PostgreSQL, Spring Data JPA | The relational model is the point of the project |
| Migrations | Flyway | One dependency, real signal, no real cost |
| AI Integration | OpenRouter behind an `AiClient` interface | Provider-agnostic, free tier available for dev |
| API Docs | springdoc-openapi (Swagger UI) | Matches your CV |
| Testing | JUnit 5 + Mockito, mocked `AiClient` | Fast, no real API calls needed in CI |
| Frontend | Minimal single-page (plain HTML/JS or small React app) | Exists only to demo the backend |
| Deployment | Docker + docker-compose | Recruiters can actually run it |

**Cost/practical note:** use a free OpenRouter model (`:free` suffix) during all development. Free tier is rate-limited to ~20 requests/minute — since one blueprint generation now chains 5+ AI calls, avoid rapid repeated manual testing or you'll hit that limit mid-request. Switch to a paid model (Claude, GPT) only for your final demo recording.

---

## 5. Architecture

```
Controller (REST)
   ↓
Service (orchestrates the pipeline, one method per stage, all synchronous)
   ↓
AiClient (interface)  →  OpenRouterAiClient (implementation)
   ↓
Repository (Spring Data JPA)
   ↓
PostgreSQL
```

- Prompt templates live in `resources/prompts/*.txt`, never inline in Java code.
- Every AI call demands strict JSON output from the model; parse into a DTO. If parsing fails, return a clear error — no retry (see Section 2).
- `AiClient` never leaks OpenRouter-specific types outside its implementation.

---

## 6. Data Model

```
Project
 ├─ id, name, createdAt
 └─ versions: List<ProjectVersion>

ProjectVersion
 ├─ id, project_id, versionNumber, ideaSnapshot, changeDescription (nullable, v2+)
 ├─ status (DRAFT | AWAITING_ANSWERS | COMPLETED | FAILED)
 ├─ clarifyingQuestions: List<ClarifyingQuestion>
 ├─ requirements: List<Requirement>
 ├─ epics: List<RoadmapEpic>
 └─ architectureRecommendations: List<ArchitectureRecommendation>

ClarifyingQuestion
 ├─ id, projectVersion_id, questionText, order
 └─ answer: ClarifyingAnswer (nullable until answered)

Requirement
 ├─ id, projectVersion_id, type (FUNCTIONAL | NON_FUNCTIONAL), description
 └─ changeType (ADDED | REMOVED | MODIFIED | UNCHANGED)

RoadmapEpic
 ├─ id, projectVersion_id, title, description, order
 ├─ changeType
 └─ userStories: List<UserStory>

UserStory
 ├─ id, epic_id, title, description, acceptanceCriteria
 ├─ changeType
 └─ tasks: List<Task>

Task
 ├─ id, userStory_id, title, description
 ├─ priority (HIGH | MEDIUM | LOW)
 ├─ effortEstimate (S | M | L)
 └─ changeType

ArchitectureRecommendation
 ├─ id, projectVersion_id, component
 ├─ recommendation      -- e.g. "Spring Boot + PostgreSQL"
 ├─ reasoning            -- why this fits the requirements
 ├─ tradeoffs             -- alternatives considered and why they were rejected
 └─ changeType
```

**On the "explain, don't just list" instruction:** applied fully to `ArchitectureRecommendation` (recommendation / reasoning / trade-offs) since that's where senior-level judgment is most visible. I deliberately did *not* add a `reasoning` field to every `Requirement` — for atomic functional/non-functional requirements, the rationale is usually self-evident from the description, and a rationale field on every single requirement would bloat the schema with repetitive text rather than add interview signal. Architecture and (below) roadmap prioritization are where the "senior thinking" actually shows up, so that's where the extra fields live.

**Versioning approach unchanged from before:** each `ProjectVersion` holds its own complete current set of items, not a diff-only set. On a revision, the AI receives the previous version's items as context and outputs each item with an explicit `changeType`, so removed items still appear (tagged `REMOVED`) for display purposes — no separate diff-computation service needed.

---

## 7. API Endpoints

```
POST   /api/projects
       body: { name, ideaDescription }
       → creates Project + ProjectVersion v1
       → synchronously generates clarifying questions
       → 200 OK with project id + questions

POST   /api/projects/{id}/versions/{v}/answers
       body: [{ questionId, answerText }]
       → synchronously runs the full pipeline:
         functional reqs → non-functional reqs → epics/user stories/tasks
         → architecture recommendation (with reasoning + trade-offs)
       → persists everything
       → 200 OK with the full structured blueprint
       (allow 60-90s timeout on this call specifically)

GET    /api/projects/{id}/versions/{v}
       → returns the stored blueprint

GET    /api/projects/{id}/versions/{v}/export?format=markdown

POST   /api/projects/{id}/revise
       body: { changeDescription: "Remove payment integration" }
       → creates ProjectVersion v(n+1), synchronously regenerates with
         prior version as context, tags every item's changeType
       → 200 OK with the new version

GET    /api/projects/{id}/versions
       → version history list
```

---

## 8. Weekly Plan

**Week 1 — Foundation**
- Docker Compose (Postgres), Spring Boot skeleton, Flyway migration for the full schema
- `AiClient` interface + `OpenRouterAiClient` (one working call against a free model)
- `POST /api/projects`: create project + v1, synchronously generate & persist clarifying questions
- `GET /api/projects/{id}/versions/{v}`: retrieve a stored version

**Week 2 — Core Blueprint Pipeline**
- `POST .../answers`: full synchronous pipeline (functional → non-functional → epics/stories/tasks → architecture with reasoning/trade-offs)
- Persist the full Epic → UserStory → Task hierarchy
- Clean error responses on AI/parse failure (no retry)
- Swagger docs

**Week 3 — Versioning, Export, Tests**
- `POST .../revise` with AI-computed `changeType` tagging
- Markdown export (Jira JSON if ahead of schedule)
- Unit tests (mocked `AiClient`) + a couple of integration tests on the main path

**Week 4 — Frontend & Delivery**
- Minimal frontend: idea form → questions form → blueprint view (epics/stories/tasks, architecture reasoning) → revise box → export button
- Full docker-compose stack, README (architecture diagram + note on the async evolution path below), demo GIF
- Buffer; pull from "Should have" only if on schedule

---

## 9. Future Improvements (explicitly out of scope for v1)

Worth stating plainly in the README so it reads as a roadmap, not a gap:

- **Async processing:** introduce a `GenerationJob` entity + `@Async` execution + a polling endpoint once the pipeline needs to serve concurrent users or the chained AI calls grow past what a synchronous request comfortably tolerates.
- **Retry logic:** add exponential-backoff retries for malformed AI JSON once reliability under real usage becomes a requirement rather than a demo concern.
- **Background job tracking / progress UI:** a natural follow-on once async processing exists.
- **Auth, PDF export, multi-project comparison:** as listed in the "Could have" tier.
- **If this were ever productionized:** a message broker only becomes justified at a scale this project isn't targeting — worth saying explicitly so it reads as a considered decision rather than an oversight.

---

## 10. Claude Code Master Prompt

Paste this in **Plan Mode** for your first session:

```
I'm a Java/Spring Boot backend developer building "BlueForge" for my portfolio —
a REST API that takes a plain-language software idea, asks the user a bounded
round of AI-generated clarifying questions, and then synchronously produces a
full structured blueprint: functional/non-functional requirements, a roadmap of
epics containing user stories broken into tasks (with priority and effort
estimate), and an architecture recommendation that includes reasoning and
trade-offs, not just a list. Users can later submit a revision ("remove payment
integration") and get a new version with every item tagged
ADDED/REMOVED/MODIFIED/UNCHANGED.

This is a portfolio MVP, not a production system. Deliberately keep it
synchronous — no async processing, no job entity, no polling, no retry logic.
A 10-20+ second response for the blueprint-generation endpoint is fine.

STACK:
- Java 21, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Flyway
- OpenRouter (OpenAI-compatible API) as the AI provider, behind an AiClient
  interface — nothing outside that one implementation class should know
  it's OpenRouter
- springdoc-openapi (Swagger UI), JUnit5 + Mockito

ARCHITECTURE RULES:
- Controller → Service → AiClient (interface) → Repository, strictly layered
- DTOs separate from entities
- Prompt templates in resources/prompts/*.txt, never inline in Java code
- Every AI call must demand strict JSON output; parse into a DTO; on failure,
  return a clean error to the client — do not implement retry logic
- The blueprint-generation and revise endpoints should use a longer HTTP
  client timeout (60-90s) since they chain multiple AI calls
- OpenRouter API key and model name both come from application.yml /
  environment variables, never hardcoded

THIS SESSION'S GOAL (do only this, nothing further):
1. Project skeleton with the dependencies above
2. Entity model: Project, ProjectVersion, ClarifyingQuestion, ClarifyingAnswer
   (see attached data model for fields; leave Requirement/Epic/UserStory/
   Task/ArchitectureRecommendation for next session)
3. Flyway migration for this initial schema
4. AiClient interface + OpenRouterAiClient implementation with one working
   method that sends a prompt and returns raw text (test against a free
   OpenRouter model)
5. POST /api/projects: creates Project + ProjectVersion v1, synchronously
   calls AiClient to generate 3-7 clarifying questions, persists them,
   returns them in the response
6. GET /api/projects/{id}/versions/{v}: returns the stored version

Show me your plan (file structure, order of work) before writing code.
After each step, run the relevant tests and show me the output.
```

For each following week, write a similarly-scoped prompt referencing this document's Weekly Plan, and paste the relevant Data Model / API Endpoints excerpt as context.

---

## 11. CLAUDE.md

```markdown
# BlueForge

## What it does
Turns a plain-language software idea into a structured blueprint — requirements,
an epic/user-story/task roadmap, and a reasoned architecture recommendation —
via a bounded AI clarification workflow. Supports versioned revisions with
change tracking.

## Commands
- `./mvnw spring-boot:run`
- `./mvnw test`
- `docker-compose up`

## Architecture
- Controller → Service → AiClient (interface) → Repository
- AI provider: OpenRouter, OpenAI-compatible, model name in config
- Prompt templates: resources/prompts/
- Deliberately synchronous — no async, no job tracking, no retry logic in v1
  (see "Future Improvements" in the project plan for the evolution path)

## Code Standards
- DTOs separate from entities
- Every AI response is parsed as strict JSON; on failure, return a clean
  error — do not add retry logic
- Prompt text never inline in Java code
- New endpoint → at least one test

## Don't
- Hardcode API keys
- Put business logic in controllers
- Add @Async, job entities, polling, retry logic, or message brokers —
  deliberately out of scope for this version
```

---

## 12. Interview Framing

- "I kept the pipeline synchronous deliberately — for a single-user demo, async job tracking would have been complexity simulating a scale problem I don't have."
- "The architecture recommendation includes reasoning and trade-offs, not just a choice, because that's what actually distinguishes a solution architect's output from a list."
- "I let the model compute semantic diffs between versions instead of writing text-matching code."
- "The AI layer is provider-agnostic from day one — switching from a free model in development to Claude for the final demo is a one-line config change."
